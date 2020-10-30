package com.agile4j.feed.builder

import com.agile4j.utils.util.CollectionUtil
import org.apache.commons.lang3.StringUtils.isBlank
import org.slf4j.LoggerFactory
import java.util.stream.Collectors.toSet

/**
 * @param S sortType 排序项类型
 * @param I indexType 索引类型
 * @param A accompanyType 伴生资源类型
 * @param T targetType 映射目标类型
 * @author liurenpeng
 * Created on 2020-08-07
 */
class FeedBuilder<S: Number, I, A, T> internal constructor(
    private val supplier: (S, Int) -> List<Pair<I, S>>,
    private val searchCount: Int,
    private val maxSearchCount: Int,
    private val searchBufferSize: Int,
    private val searchTimesLimit: Int,
    private val maxSearchBatchSize: Int,
    private val topNSupplier: () -> List<I>,
    private val fixedSupplierMap: MutableMap<FixedPosition, () -> List<I>>,
    private val builder: ((Collection<I>) -> Map<I, A>)?,
    private val mapper: ((Collection<A>) -> Map<A, T>)?,
    private val filter: (A) -> Boolean,
    private val targetFilter: (T) -> Boolean,
    private val sortEncoder: (S) -> String,
    private val sortDecoder: (String) -> S,
    private val indexEncoder: (I) -> String,
    private val indexDecoder: (String) -> I,
    private val sortInitValue: S,
    private val indexInitValue: I,
    private val sortComparator: Comparator<S>,
    private val indexComparator: Comparator<I>,
    private val sortType: SortType,

    private val maxFixedPosition: Int

) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private lateinit var indexFilter: (I) -> Boolean

    fun buildBy(cursorStr: String?): FeedBuilderResponse<T> = buildBy(cursorStr, searchCount)

    /**
     * @param cursorStr 第一次请求传入""，后续请求透传上次请求返回的[FeedBuilderResponse.nextCursor]
     * @param searchCount 本次查询条数 必须大于等于最大固定资源位位置，否则抛出[IllegalArgumentException]
     */
    fun buildBy(cursorStr: String?, searchCount: Int): FeedBuilderResponse<T> {
        val realSearchCount = Math.min(searchCount, maxSearchCount)
        if (realSearchCount < maxFixedPosition) throw IllegalArgumentException(
            "searchCount值($realSearchCount)必须大于等于maxFixedPosition($maxFixedPosition)")

        val cursor = decodeCursor(cursorStr)
        indexFilter = { !cursor.showedRandomIndices.contains(it) }

        return when {
            cursor.isNoMore() -> FeedBuilderResponse.noMoreInstance()
            cursor.isTail() -> buildTailPosition(cursor, realSearchCount)
            else -> buildHeadPosition(cursor, realSearchCount)
        }
    }

    private fun buildHeadPosition(
        cursor: FeedBuilderCursor<S, I>,
        searchCount: Int
    ): FeedBuilderResponse<T>  {
        // TODO

        return FeedBuilderResponse(emptyList(), NO_MORE_CURSOR_STR)
    }

    private fun buildTailPosition(
        cursor: FeedBuilderCursor<S, I>,
        searchCount: Int
    ): FeedBuilderResponse<T>  {
        val fetchedTargetList = mutableListOf<T>()
        val fetchedIndexList = mutableSetOf<I>()

        var dataEnd = false
        var sortOffset = cursor.sort
        var searchTimes = 1

        // 去重用
        var boundarySortVal = cursor.sort
        var boundaryIndexVal = cursor.index

        var indexToSort = supplier.invoke(sortOffset, searchCount + 1)
        while (CollectionUtil.isNotEmpty(indexToSort)) {
            if (searchTimes >= searchTimesLimit) {
                logger.warn("searchTimes over limit. searchTimes:{} searchTimeLimit:{}",
                    searchTimes, searchTimesLimit)
                return FeedBuilderResponse.noMoreInstance()
            }

            if (indexToSort.size < searchCount + 1) {
                dataEnd = true
            } else {
                val currSortOffset = indexToSort[searchCount].second
                if (currSortOffset == sortOffset) {
                    // 说明同offset的记录条数超出searchCount了，策略是先全部查出当前offset的记录，再offset--/++
                    indexToSort = supplier.invoke(sortOffset, maxSearchCount)
                    sortOffset = if (sortType == SortType.DESC)
                        decrementSortVal(sortOffset) else incrementSortVal(sortOffset)
                } else {
                    sortOffset = currSortOffset
                }
            }

            indexToSort = indexToSort.filter { indexFilter.invoke(it.first) }
            val targetToSort = rendAndFilter(indexToSort)

            targetToSort.forEach{ dto ->
                val repeated: Boolean = if (sortType == SortType.DESC) {
                    (sortComparator.compare(dto.sort, cursor.sort) == 0
                            && indexComparator.compare(dto.index, cursor.index) > 0)
                            || (sortComparator.compare(dto.sort, boundarySortVal) == 0
                            && indexComparator.compare(dto.index, boundaryIndexVal) > 0)
                            || (fetchedIndexList.contains(dto.index))
                } else {
                    (sortComparator.compare(dto.sort, cursor.sort) == 0
                            && indexComparator.compare(dto.index, cursor.index) < 0)
                            || (sortComparator.compare(dto.sort, boundarySortVal) == 0
                            && indexComparator.compare(dto.index, boundaryIndexVal) < 0)
                            || (fetchedIndexList.contains(dto.index))
                }
                if (repeated) return@forEach

                if (fetchedTargetList.size == searchCount) {
                    return FeedBuilderResponse(fetchedTargetList, encodeCursor(
                        FeedBuilderCursor(Position.TAIL, dto.sort, dto.index, cursor.showedRandomIndices)))
                } else {
                    fetchedTargetList.add(dto.target)
                    fetchedIndexList.add(dto.index)
                    if (sortType == SortType.DESC) {
                        if (sortComparator.compare(dto.sort, boundarySortVal) <= 0) {
                            boundarySortVal = dto.sort
                            boundaryIndexVal = dto.index
                        }
                    } else {
                        if (sortComparator.compare(dto.sort, boundarySortVal) >= 0) {
                            boundarySortVal = dto.sort
                            boundaryIndexVal = dto.index
                        }
                    }
                }
            }

            indexToSort = if (dataEnd) emptyList() else supplier.invoke(sortOffset, searchCount + 1)
            searchTimes++
        }

        return FeedBuilderResponse.noMoreInstance()
    }

    private fun rendAndFilter(indexToSorts: List<Pair<I, S>>): List<ResourceDTO<S, I, T>> {
        if (CollectionUtil.isEmpty(indexToSorts)) return emptyList()

        // TODO 可集成agile4j-model-builder的逻辑

        val indices = indexToSorts.map { it.first }
        val indexToAccompany = builder?.invoke(indices) ?: return emptyList()
        val accompanies = indexToAccompany.values.stream().filter(filter).collect(toSet())
        if (CollectionUtil.isEmpty(accompanies)) return emptyList()

        val accompanyToTarget = mapper?.invoke(accompanies) ?: return emptyList()
        val targets = accompanyToTarget.values.stream().filter(targetFilter).collect(toSet())
        if (CollectionUtil.isEmpty(targets)) return emptyList()

        val result: MutableList<ResourceDTO<S, I, T>> = mutableListOf()
        indexToSorts.forEach{ i2s ->
            val a = indexToAccompany[i2s.first] ?: return@forEach
            val t = accompanyToTarget[a] ?: return@forEach
            result.add(ResourceDTO(i2s.second, i2s.first, t))
        }
        return result
    }

    class ResourceDTO<S: Number, I, T>(val sort: S, val index: I, val target: T)

    @Suppress("UNCHECKED_CAST")
    private fun decrementSortVal(s: S): S =
        when (s) {
            is Double -> (s - 1) as S
            is Float -> (s - 1) as S
            is Long -> (s - 1) as S
            is Int -> (s - 1) as S
            is Short -> (s - 1) as S
            is Byte -> (s - 1) as S
            else -> throw IllegalArgumentException("illegal sortType")
        }

    @Suppress("UNCHECKED_CAST")
    private fun incrementSortVal(s: S): S =
        when (s) {
            is Double -> (s + 1) as S
            is Float -> (s + 1) as S
            is Long -> (s + 1) as S
            is Int -> (s + 1) as S
            is Short -> (s + 1) as S
            is Byte -> (s + 1) as S
            else -> throw IllegalArgumentException("illegal sortType")
        }

    private fun buildInitCursor(): FeedBuilderCursor<S, I> = FeedBuilderCursor(
        Position.TOP, sortInitValue, indexInitValue, mutableSetOf())

    private fun encodeCursor(cursor: FeedBuilderCursor<S, I>): String {
        val positionStr = cursor.position.name
        val sortStr = sortEncoder.invoke(cursor.sort)
        val indexStr = indexEncoder.invoke(cursor.index)
        val showedRandomIndicesStr = cursor.showedRandomIndices
            .joinToString(INDEX_SEPARATOR, transform = indexEncoder)
        return listOf(positionStr, sortStr, indexStr, showedRandomIndicesStr)
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
        if (splitList.size != 4) throw IllegalArgumentException("cursor格式错误:$cursorStr")

        val positionStr = splitList[0]
        val sortStr = splitList[1]
        val indexStr = splitList[2]
        val showedRandomIndicesStr = splitList[3]

        val position = Position.ofName(positionStr)
            ?: throw IllegalArgumentException("cursor格式错误:$cursorStr")
        val sort = sortDecoder.invoke(sortStr)
        val index = indexDecoder.invoke(indexStr)
        val showedRandomIndices: MutableSet<I> = if (isBlank(showedRandomIndicesStr)) mutableSetOf() else {
            val showedRandomIndexSplitList = showedRandomIndicesStr.split(INDEX_SEPARATOR)
            showedRandomIndexSplitList.map(indexDecoder).toMutableSet()
        }

        return FeedBuilderCursor(position, sort, index, showedRandomIndices)
    }

}


