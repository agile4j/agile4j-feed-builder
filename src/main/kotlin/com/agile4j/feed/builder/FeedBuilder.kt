package com.agile4j.feed.builder

import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.lang3.math.NumberUtils

/**
 * @param S sortType 排序项类型
 * @param I indexType 索引类型
 * @param A accompanyType 伴生资源类型
 * @param T targetType 映射目标类型
 * @author liurenpeng
 * Created on 2020-08-07
 */
class FeedBuilder<S, I, A, T> internal constructor(
    private val supplier: (S, Int) -> List<I>,
    private val searchCount: Int,
    private val maxSearchCount: Int,
    private val searchBufferSize: Int,
    private val searchTimesLimit: Int,
    private val maxSearchBatchSize: Int,
    private val topNSupplier: () -> List<I>,
    private val fixedSupplierMap: MutableMap<FixedPosition, () -> List<I>>,
    private val builder: ((Collection<I>) -> Map<I, A>)?,
    private val mapper: ((Collection<A>) -> Collection<T>)?,
    private val filter: (T) -> Boolean,
    private val sortEncoder: (S) -> String,
    private val sortDecoder: (String) -> S,
    private val indexEncoder: (I) -> String,
    private val indexDecoder: (String) -> I,
    private val sortInitValue: S,
    private val indexInitValue: I

) {

    fun buildBy(cursorStr: String?): FeedBuilderResponse<T> = buildBy(cursorStr, searchCount)

    /**
     * @param cursorStr 第一次请求传入""，后续请求透传上次请求返回的[FeedBuilderResponse.nextCursor]
     * @param searchCount 本次查询条数 必须大于等于最大固定资源位位置，否则抛出[IllegalArgumentException]
     */
    fun buildBy(cursorStr: String?, searchCount: Int): FeedBuilderResponse<T> {
        val maxFixedPosition = fixedSupplierMap.keys.maxBy { it.number }?.number?:0
        val realSearchCount = Math.min(searchCount, maxSearchCount)
        if (realSearchCount < maxFixedPosition) throw IllegalArgumentException(
            "searchCount值($realSearchCount)必须大于等于maxFixedPosition($maxFixedPosition)")

        val cursor = decodeCursor(cursorStr)
        if (cursor.isNoMore()) return FeedBuilderResponse.noMoreInstance()

        //val isNotMoreThanConf =
        // TODO
        return FeedBuilderResponse(emptyList(), NO_MORE_CURSOR_STR)
    }

    private fun buildInitCursor(): FeedBuilderCursor<S, I> = FeedBuilderCursor(
        Position.TOP, 0, sortInitValue, indexInitValue, mutableSetOf())

    private fun encodeCursor(cursor: FeedBuilderCursor<S, I>): String {
        val positionStr = cursor.position.name
        val cumulativeRespCountStr = cursor.cumulativeRespCount.toString()
        val sortStr = sortEncoder.invoke(cursor.sort)
        val indexStr = indexEncoder.invoke(cursor.index)
        val showedRandomIndicesStr = cursor.showedRandomIndices
            .joinToString(INDEX_SEPARATOR, transform = indexEncoder)
        return listOf(positionStr, cumulativeRespCountStr, sortStr, indexStr, showedRandomIndicesStr)
            .joinToString(CURSOR_SEPARATOR)
    }

    /**
     * @param cursorStr 格式示例 TOP;10;2492;96789002;90009623,32397452,94994452
     * @throws IllegalArgumentException cursorStr格式错误
     */
    private fun decodeCursor(cursorStr: String?): FeedBuilderCursor<S, I> {
        if (isBlank(cursorStr)) {
            return buildInitCursor()
        }

        val splitList = cursorStr!!.split(CURSOR_SEPARATOR)
        if (splitList.size != 5) throw IllegalArgumentException("cursor格式错误:$cursorStr")

        val positionStr = splitList[0]
        val cumulativeRespCountStr = splitList[1]
        val sortStr = splitList[2]
        val indexStr = splitList[3]
        val showedRandomIndicesStr = splitList[4]

        val position = Position.ofName(positionStr)
            ?: throw IllegalArgumentException("cursor格式错误:$cursorStr")
        val cumulativeRespCount = NumberUtils.toInt(cumulativeRespCountStr)
        val sort = sortDecoder.invoke(sortStr)
        val index = indexDecoder.invoke(indexStr)
        val showedRandomIndices: MutableSet<I> = if (isBlank(showedRandomIndicesStr)) mutableSetOf() else {
            val showedRandomIndexSplitList = showedRandomIndicesStr.split(INDEX_SEPARATOR)
            showedRandomIndexSplitList.map(indexDecoder).toMutableSet()
        }

        return FeedBuilderCursor(position, cumulativeRespCount, sort, index, showedRandomIndices)
    }

}


