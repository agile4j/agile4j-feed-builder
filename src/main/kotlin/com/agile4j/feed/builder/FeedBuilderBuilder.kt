package com.agile4j.feed.builder

import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.indexBy
import kotlin.reflect.KClass

/**
 * @param S sortType 排序项类型
 * @param I indexType 索引类型
 * @param A accompanyType 伴生资源类型
 * @param T targetType 映射目标类型
 * @author liurenpeng
 * @date Created in 20-10-27
 */
class FeedBuilderBuilder<S: Number, I: Any, A: Any, T: Any>(
    private val indexClass: KClass<I>,
    private val accompanyClass: KClass<A>,
    private val targetClass: KClass<T>,
    private val supplier: (S, Int) -> List<Pair<I, S>>,
    private val sortEncoder: (S) -> String,
    private val sortDecoder: (String) -> S,
    private val indexEncoder: (I) -> String,
    private val indexDecoder: (String) -> I,
    private val sortInitValue: () -> S,
    private val indexInitValue: () -> I,
    private val sortComparator: Comparator<S>,
    private val indexComparator: Comparator<I>,
    private val sortType: SortType) {

    private var searchCount: () -> Int = { DEFAULT_SEARCH_COUNT }
    private var maxSearchCount: () -> Int = { DEFAULT_MAX_SEARCH_COUNT }
    private var searchBufferSize: () -> Int = { DEFAULT_SEARCH_BUFFER_SIZE }
    private var searchTimesLimit: () -> Int = { DEFAULT_SEARCH_TIMES_LIMIT }
    private var maxSearchBatchSize: () -> Int = { DEFAULT_MAX_SEARCH_BATCH_SIZE }
    private var topNSupplier: () -> List<I> = ::emptyList
    private var fixedSupplierMap: MutableMap<FixedPosition, () -> List<I>> = mutableMapOf()
    private var builder: ((Collection<I>) -> Map<I, A>)? = null
    private var mapper: ((Collection<A>) -> Map<A, T>)? = null
    private var indexFilter: (I) -> Boolean = { true }
    private var batchIndexFilter: (Collection<I>) -> Map<I, Boolean> = { it.associateWith { true } }
    private var filter: (A) -> Boolean = { true }
    private var targetFilter: (T) -> Boolean = { true }

    fun build(): FeedBuilder<S, I, A, T> {
        if (maxSearchCount.invoke() < searchCount.invoke()) throw IllegalArgumentException(
            "maxSearchCount值($maxSearchCount)必须大于等于searchCount($searchCount)")
        val maxFixedPosition = fixedSupplierMap.keys.maxBy { it.number }?.number?:0
        if (searchCount.invoke() < maxFixedPosition) throw IllegalArgumentException(
            "searchCount值($searchCount)必须大于等于maxFixedPosition($maxFixedPosition)")

        return FeedBuilder(indexClass, accompanyClass, targetClass,
            supplier, searchCount, maxSearchCount, searchBufferSize, searchTimesLimit,
            maxSearchBatchSize, topNSupplier, fixedSupplierMap, builder, mapper,
            indexFilter, batchIndexFilter, filter, targetFilter, sortEncoder, sortDecoder,
            indexEncoder, indexDecoder, sortInitValue, indexInitValue,
            sortComparator, indexComparator, sortType, maxFixedPosition)
    }

    /**
     * 每次获取的资源条数
     * 默认值[DEFAULT_SEARCH_COUNT]
     */
    fun searchCount(searchCount: () -> Int): FeedBuilderBuilder<S, I, A, T> {
        this.searchCount = searchCount
        return this
    }

    /**
     * 每次获取的最大资源条数
     * [searchCount]最终值=Math.min(searchCount, maxSearchCount)
     * 默认值[DEFAULT_MAX_SEARCH_COUNT]
     */
    fun maxSearchCount(maxSearchCount: () -> Int): FeedBuilderBuilder<S, I, A, T> {
        this.maxSearchCount = maxSearchCount
        return this
    }

    /**
     * 为避免读时过滤导致多次查询增加的额外查询条数
     * 默认值[DEFAULT_SEARCH_BUFFER_SIZE]
     */
    fun searchBufferSize(searchBufferSize: () -> Int): FeedBuilderBuilder<S, I, A, T> {
        this.searchBufferSize = searchBufferSize
        return this
    }

    /**
     * 为避免死循环限制一次请求最多获取资源次数
     * 默认值[DEFAULT_SEARCH_TIMES_LIMIT]
     */
    fun searchTimesLimit(searchTimesLimit: () -> Int): FeedBuilderBuilder<S, I, A, T> {
        this.searchTimesLimit = searchTimesLimit
        return this
    }

    /**
     * 当排序项相同的资源条量大于“每次获取的资源条数”时，一次性把该排序项对应值下的资源全部取出时的limit大数值
     * 默认值[DEFAULT_MAX_SEARCH_BATCH_SIZE]
     */
    fun maxSearchBatchSize(maxSearchBatchSize: () -> Int): FeedBuilderBuilder<S, I, A, T> {
        this.maxSearchBatchSize = maxSearchBatchSize
        return this
    }

    /**
     * topN资源
     */
    fun topNSupplier(topNSupplier: () -> List<I>): FeedBuilderBuilder<S, I, A, T> {
        this.topNSupplier = topNSupplier
        return this
    }

    /**
     * 固定位置资源
     * 注意：对固定位置资源的处理是先随机出1个index，然后读时过滤。因此有被滤掉的可能，要求配置的资源尽量可用
     * @param fixedFixedPosition 使用枚举的目的：1.收敛有效值；2.屏蔽下标从0/1开始的实现细节
     * @param fixedSupplier 从中随机抽取1个资源
     */
    fun fixedSupplier(
        fixedFixedPosition: FixedPosition,
        fixedSupplier: () -> List<I>
    ): FeedBuilderBuilder<S, I, A, T> {
        this.fixedSupplierMap[fixedFixedPosition] = fixedSupplier
        return this
    }

    /**
     * 资源构建器
     *
     * 注意：
     *  1. 如果[A]在agile4j-model-builder中对应Accompany的角色，
     *  且在当前上下文中，已通过agile4j-model-builder的[indexBy]API进行过[I]、[A]关系的声明，
     *  且通过[buildBy]API进行过builder的声明，则不需要再通过该方法声明资源构建器，
     *  agile4j-feed-builder会将构建过程托管给agile4j-model-builder
     *
     *  2. 如果通过该方法声明了资源构建器，则无论是否可将构建过程托管给agile4j-model-builder，都不会托管
     *
     *  3. 托管给agile4j-model-builder，多次构建缓存互通，有性能优势
     *
     *  4. 如果当前业务不区分index和accompany的概念，即[I]、[A]为相同类型，则参数可传入：
     *  indices -> indices.associateWith { it }。即得到一个k、v相同的map
     *
     *  @see [agile4j-model-builder](https://github.com/agile4j/agile4j-model-builder)
     */
    fun builder(
        builder: (Collection<I>) -> Map<I, A>
    ): FeedBuilderBuilder<S, I, A, T> {
        this.builder = builder
        return this
    }

    /**
     * 资源映射器
     *
     * 注意：
     *  1. 如果[A]在agile4j-model-builder中对应Accompany的角色，
     *  且在当前上下文中，已通过agile4j-model-builder的[accompanyBy]API进行过[A]、[T]关系的声明，
     *  则不需要再通过该方法声明资源映射器，agile4j-feed-builder会将映射过程托管给agile4j-model-builder
     *
     *  2. 如果通过该方法声明了资源映射器，则无论是否可将映射过程托管给agile4j-model-builder，都不会托管
     *
     *  3. 托管给agile4j-model-builder，多次构建缓存互通，有性能优势
     *
     *  4. 如果当前业务不区分accompany和target的概念，即[A]、[T]为相同类型，则参数可传入：
     *  accompanies -> accompanies。即映射结果为其自身
     *
     *  @see [agile4j-model-builder](https://github.com/agile4j/agile4j-model-builder)
     */
    fun mapper(
        mapper: (Collection<A>) -> Map<A, T>
    ): FeedBuilderBuilder<S, I, A, T> {
        this.mapper = mapper
        return this
    }

    fun indexFilter(
        indexFilter: (I) -> Boolean
    ): FeedBuilderBuilder<S, I, A, T> {
        this.indexFilter = indexFilter
        return this
    }

    fun batchIndexFilter(
        batchIndexFilter: (Collection<I>) -> Map<I, Boolean>
    ): FeedBuilderBuilder<S, I, A, T> {
        this.batchIndexFilter = batchIndexFilter
        return this
    }

    fun filter(
        filter: (A) -> Boolean
    ): FeedBuilderBuilder<S, I, A, T> {
        this.filter = filter
        return this
    }

    fun targetFilter(
        targetFilter: (T) -> Boolean
    ): FeedBuilderBuilder<S, I, A, T> {
        this.targetFilter = targetFilter
        return this
    }
}