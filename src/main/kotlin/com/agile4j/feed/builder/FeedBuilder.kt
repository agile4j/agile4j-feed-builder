package com.agile4j.feed.builder

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.buildIndexToAccompanyWithExistModelBuilder
import com.agile4j.model.builder.buildMapOfAWithExistModelBuilder
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors.toSet
import kotlin.collections.ArrayList
import kotlin.reflect.KClass

/**
 * @param S sortType 排序项类型
 * @param I indexType 索引类型
 * @param A accompanyType 伴生资源类型
 * @param T targetType 映射目标类型
 * @author liurenpeng
 * Created on 2020-08-07
 */
class FeedBuilder<S: Number, I: Any, A: Any, T: Any> internal constructor(
    private val sortClass: KClass<S>,
    private val indexClass: KClass<I>,
    private val accompanyClass: KClass<A>,
    private val targetClass: KClass<T>,
    private val supplier: (S, Int) -> List<Pair<I, S>>,
    private val searchCount: () -> Int,
    private val maxSearchCount: () -> Int,
    private val searchBufferSize: () -> Int,
    private val searchTimesLimit: () -> Int,
    private val maxSearchBatchSize: () -> Int,
    private val topNSupplier: () -> List<I>,
    private val fixedSupplierMap: MutableMap<FixedPosition, () -> List<I>>,
    private val builder: ((Collection<I>) -> Map<I, A>)?,
    private val mapper: ((Collection<A>) -> Map<A, T>)?,
    private val indexFilter: (I) -> Boolean,
    private val batchIndexFilter: (Collection<I>) -> Map<I, Boolean>,
    private val filter: (A) -> Boolean,
    private val targetFilter: (T) -> Boolean,
    private val indexEncoder: (I) -> String,
    private val indexDecoder: (String) -> I,
    private val indexInitValue: () -> I,
    private val indexComparator: Comparator<I>,
    private val sortType: SortType,

    private val maxFixedPosition: Int

) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val random = Random()

    fun buildBy(cursorStr: String?): FeedBuilderResponse<T> = buildBy(cursorStr, searchCount.invoke())

    /**
     * @param cursorStr 第一次请求传入""，后续请求透传上次请求返回的[FeedBuilderResponse.nextCursor]
     * @param searchCount 查询条数 必须大于等于最大固定资源位位置，否则抛出[IllegalArgumentException]
     */
    fun buildBy(cursorStr: String?, searchCount: Int): FeedBuilderResponse<T> {
        val realSearchCount = Math.min(searchCount, maxSearchCount.invoke())
        if (realSearchCount < maxFixedPosition) throw IllegalArgumentException(
            "searchCount值($realSearchCount)必须大于等于maxFixedPosition($maxFixedPosition)")
        val cursor = decodeCursor(cursorStr)

        try {
            return when {
                cursor.isNoMore() -> FeedBuilderResponse.noMoreInstance()
                cursor.isTail() -> buildTailPosition(cursor, realSearchCount)
                cursor.isFirstPage -> buildFirstPage(cursor, realSearchCount)
                else -> buildHeadPosition(cursor, realSearchCount)
            }
        } finally {
            Scopes.setModelBuilder(null)
        }
    }

    private fun buildFirstPage(
        cursor: FeedBuilderCursor<S, I>,
        searchCount: Int
    ): FeedBuilderResponse<T>  {
        val topNIndices = topNSupplier.invoke()

        val fixedPositionToIndices = fixedSupplierMap
            .mapValues { it.value.invoke() }
        val fixedPositionIndices = fixedPositionToIndices.values.stream()
            .flatMap { it.stream() }.collect(toSet())

        val indices = (topNIndices + fixedPositionIndices).toSet()
        val sortInitValue = sortInitValue().invoke()
        val indexToSort = indices.map { it to sortInitValue }
        val dtoList = rendAndFilter(indexToSort, indexFilter, batchIndexFilter)
        val indexToTarget = dtoList.associateBy({ it.index }, { it.target }).toMutableMap()
        val enableIndices = indexToTarget.keys

        val fixedPositionToFetchedIndex = mutableMapOf<FixedPosition, I>()
        val fetchedFixedPositionIndices = mutableSetOf<I>()
        val enableFixedPositionIndices = fixedPositionIndices.filter { enableIndices.contains(it) }
        fixedSupplierMap.keys.forEach { currPosition ->
            val currIndices = fixedPositionToIndices[currPosition]
            if (CollectionUtil.isEmpty(currIndices)) return@forEach
            val currEnableIndices = currIndices!!.filter {
                enableFixedPositionIndices.contains(it) && !fetchedFixedPositionIndices.contains(it) }
            if (CollectionUtil.isEmpty(currEnableIndices)) return@forEach

            val fetchedIndex = currEnableIndices[random.nextInt(currEnableIndices.size)]
            fetchedFixedPositionIndices.add(fetchedIndex)
            fixedPositionToFetchedIndex[currPosition] = fetchedIndex
        }
        cursor.showedRandomIndices.addAll(fetchedFixedPositionIndices)

        val enableTopNIndices = topNIndices.filter {
            enableIndices.contains(it) && !fetchedFixedPositionIndices.contains(it) }
        val topNTargetList = enableTopNIndices.map { indexToTarget[it]!! }

        val tailDTOList = if (topNTargetList.size < searchCount + 1) {
            val needTailCount = searchCount + 1 - topNTargetList.size
            fetchTailDTOList(cursor, needTailCount)
        } else emptyList()
        tailDTOList.forEach{ indexToTarget[it.index] = it.target }
        val tailTargetList = tailDTOList.map { it.target }

        val targetList: MutableList<T> = if (!MapUtil.isEmpty(fixedPositionToFetchedIndex)) LinkedList() else
            ArrayList(topNTargetList.size + tailTargetList.size)
        targetList.addAll(topNTargetList)
        targetList.addAll(tailTargetList)
        fixedPositionToFetchedIndex.forEach{ targetList.add(it.key.number - 1, indexToTarget[it.value]!!) }

        if (targetList.size < searchCount) return FeedBuilderResponse(targetList, NO_MORE_CURSOR_STR)

        val nextTarget = targetList[searchCount]
        val nextIndex = indexToTarget.entries.filter { it.value == nextTarget }[0].key
        val nextCursor: FeedBuilderCursor<S, I> = if (enableTopNIndices.contains(nextIndex)) {
            FeedBuilderCursor(Position.TOP, sortInitValue, nextIndex, fetchedFixedPositionIndices, false)
        } else {
            val nextSort = tailDTOList.filter { it.index == nextIndex }[0].sort
            FeedBuilderCursor(Position.TAIL, nextSort, nextIndex, fetchedFixedPositionIndices, false)
        }

        return FeedBuilderResponse(targetList.subList(0, searchCount), encodeCursor(nextCursor))
    }

    private fun buildHeadPosition(
        cursor: FeedBuilderCursor<S, I>,
        searchCount: Int
    ): FeedBuilderResponse<T>  {
        val topNIndices = topNSupplier.invoke()
        val sortInitValue = sortInitValue().invoke()
        val topNIndexToSort = topNIndices.map { it to sortInitValue }
        val cursorIndexFilter: (I) -> Boolean = if (sortType == SortType.DESC) {
            { it <= cursor.index } } else { { it >= cursor.index } }
        val topNFilter: (I) -> Boolean = { indexFilter.invoke(it)
                && !cursor.showedRandomIndices.contains(it)
                && cursorIndexFilter.invoke(it)}
        val topNDTOList = rendAndFilter(topNIndexToSort, topNFilter, batchIndexFilter)
        val topNTargetList = topNDTOList.map { it.target }
        val enableTopNIndices = topNDTOList.map { it.index }

        val indexToTarget = topNDTOList.associateBy({ it.index }, { it.target }).toMutableMap()

        val tailDTOList = if (topNTargetList.size < searchCount + 1) {
            val needTailCount = searchCount + 1 - topNTargetList.size
            fetchTailDTOList(cursor, needTailCount)
        } else emptyList()
        tailDTOList.forEach{ indexToTarget[it.index] = it.target }
        val tailTargetList = tailDTOList.map { it.target }

        val targetList: MutableList<T> = ArrayList(topNTargetList.size + tailTargetList.size)
        targetList.addAll(topNTargetList)
        targetList.addAll(tailTargetList)

        if (targetList.size < searchCount) return FeedBuilderResponse(targetList, NO_MORE_CURSOR_STR)

        val nextTarget = targetList[searchCount]
        val nextIndex = indexToTarget.entries.filter { it.value == nextTarget }[0].key
        val nextCursor: FeedBuilderCursor<S, I> = if (enableTopNIndices.contains(nextIndex)) {
            FeedBuilderCursor(Position.TOP, sortInitValue, nextIndex, cursor.showedRandomIndices, false)
        } else {
            val nextSort = tailDTOList.filter { it.index == nextIndex }[0].sort
            FeedBuilderCursor(Position.TAIL, nextSort, nextIndex, cursor.showedRandomIndices, false)
        }

        return FeedBuilderResponse(targetList.subList(0, searchCount), encodeCursor(nextCursor))
    }

    private fun buildTailPosition(
        cursor: FeedBuilderCursor<S, I>,
        searchCount: Int
    ): FeedBuilderResponse<T>  {
        val dtoList = fetchTailDTOList(cursor, searchCount + 1)
        return if (dtoList.size < searchCount + 1) {
            FeedBuilderResponse.noMoreInstance(dtoList.map { it.target })
        } else {
            FeedBuilderResponse(dtoList.subList(0, searchCount).map { it.target },
                encodeCursor(FeedBuilderCursor(Position.TAIL,
                    dtoList[searchCount].sort, dtoList[searchCount].index,
                    cursor.showedRandomIndices, false)))
        }
    }

    private fun fetchTailDTOList(
        cursor: FeedBuilderCursor<S, I>,
        searchCount: Int
    ): List<ResourceDTO<S, I, T>>  {
        val fetchedDTOList = mutableListOf<ResourceDTO<S, I, T>>()
        val fetchedIndexList = mutableSetOf<I>()

        var dataEnd = false
        var sortOffset = cursor.sort
        var searchTimes = 1

        var boundarySortVal = cursor.sort
        var boundaryIndexVal = cursor.index

        val topNIndices = topNSupplier.invoke().toSet()
        val fullIndexFilter: (I) -> Boolean = { indexFilter.invoke(it)
                && !topNIndices.contains(it)
                && !cursor.showedRandomIndices.contains(it) }

        val realFetchCount = searchCount + searchBufferSize.invoke() + 1
        var indexToSort = supplier.invoke(sortOffset, realFetchCount)
        while (CollectionUtil.isNotEmpty(indexToSort)) {
            if (searchTimes >= searchTimesLimit.invoke()) {
                logger.warn("searchTimes over limit. searchTimes:{} searchTimeLimit:{}",
                    searchTimes, searchTimesLimit.invoke())
                return fetchedDTOList
            }

            if (indexToSort.size < realFetchCount) {
                dataEnd = true
            } else {
                val currSortOffset = indexToSort[realFetchCount - 1].second
                if (currSortOffset == sortOffset) {
                    // 说明同offset的记录条数超出searchCount了，策略是先全部查出当前offset的记录，再offset--/++
                    indexToSort = supplier.invoke(sortOffset, maxSearchBatchSize.invoke())
                    sortOffset = if (sortType == SortType.DESC)
                        decrementSortVal(sortOffset) else incrementSortVal(sortOffset)
                } else {
                    sortOffset = currSortOffset
                }
            }

            val dtoList = rendAndFilter(indexToSort, fullIndexFilter, batchIndexFilter)
            dtoList.forEach{ dto ->
                val repeated: Boolean = if (sortType == SortType.DESC) {
                    (dto.sort == cursor.sort && dto.index > cursor.index)
                            || (dto.sort == boundarySortVal && dto.index > boundaryIndexVal)
                            || (fetchedIndexList.contains(dto.index))
                } else {
                    (dto.sort ==  cursor.sort && dto.index < cursor.index)
                            || (dto.sort == boundarySortVal && dto.index < boundaryIndexVal)
                            || (fetchedIndexList.contains(dto.index))
                }
                if (repeated) return@forEach

                if (fetchedDTOList.size == searchCount) {
                    return fetchedDTOList
                } else {
                    fetchedDTOList.add(dto)
                    fetchedIndexList.add(dto.index)
                    if (sortType == SortType.DESC) {
                        if (dto.sort <= boundarySortVal) {
                            boundarySortVal = dto.sort
                            boundaryIndexVal = dto.index
                        }
                    } else {
                        if (dto.sort >= boundarySortVal) {
                            boundarySortVal = dto.sort
                            boundaryIndexVal = dto.index
                        }
                    }
                }
            }

            indexToSort = if (dataEnd) emptyList() else supplier.invoke(sortOffset, realFetchCount)
            searchTimes++
        }

        return fetchedDTOList
    }

    private operator fun S.compareTo(s: S): Int = sortComparator().compare(this, s)
    private operator fun I.compareTo(i: I): Int = indexComparator.compare(this, i)

    private fun rendAndFilter(
        originIndexToSort: List<Pair<I, S>>,
        indexFilter: (I) -> Boolean,
        batchIndexFilter: (Collection<I>) -> Map<I, Boolean>
    ): List<ResourceDTO<S, I, T>> {
        val indexToSort = filterByIndex(originIndexToSort, indexFilter, batchIndexFilter)
        if (CollectionUtil.isEmpty(indexToSort)) return emptyList()

        return if (BuildContext.checkRelation(indexClass, accompanyClass, targetClass)) {
            renderAndFilterByModelBuilder(indexToSort)
        } else {
            renderAndFilterByBuilderAndMapper(indexToSort)
        }
    }

    private fun filterByIndex(
        originIndexToSort: List<Pair<I, S>>,
        indexFilter: (I) -> Boolean,
        batchIndexFilter: (Collection<I>) -> Map<I, Boolean>
    ): List<Pair<I, S>> {
        if (CollectionUtil.isEmpty(originIndexToSort)) return emptyList()
        val indexToSort = originIndexToSort.filter { indexFilter.invoke(it.first) }
        if (CollectionUtil.isEmpty(indexToSort)) return emptyList()
        val indexToEnable = batchIndexFilter.invoke(indexToSort.map { it.first }.toSet())
        return indexToSort.filter { indexToEnable.getOrDefault(it.first, false) }
    }

    private fun renderAndFilterByBuilderAndMapper(
        indexToSort: List<Pair<I, S>>
    ): List<ResourceDTO<S, I, T>> {
        val indices = indexToSort.map { it.first }
        val indexToAccompany = builder?.invoke(indices) ?: return emptyList()
        val enableAccompanies = indexToAccompany.values.stream()
            .filter{ it != null }.filter(filter).collect(toSet())
        if (CollectionUtil.isEmpty(enableAccompanies)) return emptyList()

        val accompanyToTarget = mapper?.invoke(enableAccompanies) ?: return emptyList()
        val enableTargets = accompanyToTarget.values.stream()
            .filter{ it != null }.filter(targetFilter).collect(toSet())
        if (CollectionUtil.isEmpty(enableTargets)) return emptyList()

        return buildDTOList(indexToSort, indexToAccompany,
            enableAccompanies, accompanyToTarget, enableTargets)
    }

    private fun buildDTOList(
        indexToSort: List<Pair<I, S>>,
        indexToAccompany: Map<I, A>,
        enableAccompanies: MutableSet<A>,
        accompanyToTarget: Map<A, T>,
        enableTargets: MutableSet<T>
    ): MutableList<ResourceDTO<S, I, T>> {
        val result: MutableList<ResourceDTO<S, I, T>> = mutableListOf()
        indexToSort.forEach { i2s ->
            val a = indexToAccompany[i2s.first] ?: return@forEach
            if (!enableAccompanies.contains(a)) return@forEach
            val t = accompanyToTarget[a] ?: return@forEach
            if (!enableTargets.contains(t)) return@forEach
            result.add(ResourceDTO(i2s.second, i2s.first, t))
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun renderAndFilterByModelBuilder(
        indexToSort: List<Pair<I, S>>
    ): List<ResourceDTO<S, I, T>> {
        val modelBuilder = Scopes.getModelBuilderWithInit()

        val indices = indexToSort.map { it.first }
        val indexToAccompany = buildIndexToAccompanyWithExistModelBuilder(
            modelBuilder, accompanyClass, indices)
        val enableAccompanies = indexToAccompany.values.stream()
            .filter{ it != null }.filter(filter).collect(toSet())
        if (CollectionUtil.isEmpty(enableAccompanies)) return emptyList()

        val accompanyToTarget = buildMapOfAWithExistModelBuilder(
            modelBuilder, targetClass, enableAccompanies) as Map<A, T>
        val enableTargets = accompanyToTarget.values.stream()
            .filter{ it != null }.filter(targetFilter).collect(toSet())
        if (CollectionUtil.isEmpty(enableTargets)) return emptyList()

        return buildDTOList(indexToSort, indexToAccompany,
            enableAccompanies, accompanyToTarget, enableTargets)
    }

    class ResourceDTO<S: Number, I, T>(val sort: S, val index: I, val target: T)

    @Suppress("UNCHECKED_CAST")
    private fun sortComparator(): Comparator<S> =
        when (sortClass) {
            Double::class -> (Comparator.comparingDouble { it: Double -> it } as Comparator<S>)
            Float::class -> (Comparator.comparingDouble { it: Float -> it.toDouble() } as Comparator<S>)
            Long::class -> (Comparator.comparingLong { it: Long -> it } as Comparator<S>)
            Int::class -> (Comparator.comparingInt { it: Int -> it } as Comparator<S>)
            Short::class -> (Comparator.comparingInt { it: Short -> it.toInt() } as Comparator<S>)
            Byte::class -> (Comparator.comparingInt { it: Byte -> it.toInt() } as Comparator<S>)
            else -> throw IllegalArgumentException("illegal sortType")
        }

    @Suppress("UNCHECKED_CAST")
    private fun sortInitValue(): () -> S =
        if (sortType == SortType.DESC) {
            when (sortClass) {
                Double::class -> ({ Double.MAX_VALUE } as () -> S)
                Float::class -> ({ Float.MAX_VALUE } as () -> S)
                Long::class -> ({ Long.MAX_VALUE } as () -> S)
                Int::class -> ({ Int.MAX_VALUE } as () -> S)
                Short::class -> ({ Short.MAX_VALUE } as () -> S)
                Byte::class -> ({ Byte.MAX_VALUE } as () -> S)
                else -> throw IllegalArgumentException("illegal sortType")
            }
        } else {
            when (sortClass) {
                Double::class -> ({ Double.MIN_VALUE } as () -> S)
                Float::class -> ({ Float.MIN_VALUE } as () -> S)
                Long::class -> ({ Long.MIN_VALUE } as () -> S)
                Int::class -> ({ Int.MIN_VALUE } as () -> S)
                Short::class -> ({ Short.MIN_VALUE } as () -> S)
                Byte::class -> ({ Byte.MIN_VALUE } as () -> S)
                else -> throw IllegalArgumentException("illegal sortType")
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun sortEncoder(): (S) -> String =
        when (sortClass) {
            Double::class -> Double::toString as (S) -> String
            Float::class -> Float::toString as (S) -> String
            Long::class -> Long::toString as (S) -> String
            Int::class -> Int::toString as (S) -> String
            Short::class -> Short::toString as (S) -> String
            Byte::class -> Byte::toString as (S) -> String
            else -> throw IllegalArgumentException("illegal sortType")
        }

    @Suppress("UNCHECKED_CAST")
    private fun sortDecoder(): (String) -> S =
        when (sortClass) {
            Double::class -> ( {str: String -> NumberUtils.toDouble(str) } as (String) -> S )
            Float::class -> ( {str: String -> NumberUtils.toFloat(str) } as (String) -> S )
            Long::class -> ( {str: String -> NumberUtils.toLong(str) } as (String) -> S )
            Int::class -> ( {str: String -> NumberUtils.toInt(str) } as (String) -> S )
            Short::class -> ( {str: String -> NumberUtils.toShort(str) } as (String) -> S )
            Byte::class -> ( {str: String -> NumberUtils.toByte(str) } as (String) -> S )
            else -> throw IllegalArgumentException("illegal sortType")
        }

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
        Position.TOP, sortInitValue().invoke(), indexInitValue.invoke(), mutableSetOf(), true)

    private fun encodeCursor(cursor: FeedBuilderCursor<S, I>): String {
        val positionStr = cursor.position.name
        val sortStr = sortEncoder().invoke(cursor.sort)
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
        val sort = sortDecoder().invoke(sortStr)
        val index = indexDecoder.invoke(indexStr)
        val showedRandomIndices: MutableSet<I> = if (isBlank(showedRandomIndicesStr)) mutableSetOf() else {
            val showedRandomIndexSplitList = showedRandomIndicesStr.split(INDEX_SEPARATOR)
            showedRandomIndexSplitList.map(indexDecoder).toMutableSet()
        }

        return FeedBuilderCursor(position, sort, index, showedRandomIndices, false)
    }

}


