package com.agile4j.feed.builder

import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.indexBy

/**
 * @param S sortType 排序项类型
 * @param I indexType 索引类型
 * @param A accompanyType 伴生资源类型
 * @param T targetType 映射目标类型
 * @author liurenpeng
 * @date Created in 20-10-27
 */
class FeedBuilderBuilder<S, I, A, T>(supplier: (S, Int) -> List<I>) {

    private val feedBuilder: FeedBuilder<S, I, A, T> = FeedBuilder(supplier)

    fun build(): FeedBuilder<S, I, A, T> {
        // 校验
        // 1. fixedSupplierMap key必须大于1
        // 2. fixedSupplierMap key必须小于等于searchCount
        return feedBuilder
    }

    /**
     * 每次获取的资源条数
     * 默认值[DEFAULT_SEARCH_COUNT]
     */
    fun searchCount(searchCount: Int): FeedBuilderBuilder<S, I, A, T> {
        feedBuilder.searchCount = searchCount
        return this
    }

    /**
     * 为避免读时过滤导致多次查询增加的额外查询条数
     * 默认值[DEFAULT_SEARCH_BUFFER_SIZE]
     */
    fun searchBufferSize(searchBufferSize: Int): FeedBuilderBuilder<S, I, A, T> {
        feedBuilder.searchBufferSize = searchBufferSize
        return this
    }

    /**
     * 为避免死循环限制一次请求最多获取资源次数
     * 默认值[DEFAULT_SEARCH_TIMES_LIMIT]
     */
    fun searchTimesLimit(searchTimesLimit: Int): FeedBuilderBuilder<S, I, A, T> {
        feedBuilder.searchTimesLimit = searchTimesLimit
        return this
    }

    /**
     * 当排序项相同的资源条量大于“每次获取的资源条数”时，一次性把该排序项对应值下的资源全部取出时的limit大数值
     * 默认值[DEFAULT_MAX_SEARCH_BATCH_SIZE]
     */
    fun maxSearchBatchSize(maxSearchBatchSize: Int): FeedBuilderBuilder<S, I, A, T> {
        feedBuilder.maxSearchBatchSize = maxSearchBatchSize
        return this
    }

    /**
     * topN资源
     */
    fun topNSupplier(topNSupplier: () -> List<I>): FeedBuilderBuilder<S, I, A, T> {
        feedBuilder.topNSupplier = topNSupplier
        return this
    }

    /**
     * 固定位置资源
     * @param fixedPosition 使用枚举的目的：1.收敛有效值；2.屏蔽下标从0/1开始的实现细节
     * @param fixedSupplier 从中随机抽取1个资源
     */
    fun fixedSupplier(
        fixedPosition: Position,
        fixedSupplier: () -> List<I>
    ): FeedBuilderBuilder<S, I, A, T> {
        feedBuilder.fixedSupplierMap[fixedPosition] = fixedSupplier
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
        feedBuilder.builder = builder
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
        mapper: (Collection<A>) -> Collection<T>
    ): FeedBuilderBuilder<S, I, A, T> {
        feedBuilder.mapper = mapper
        return this
    }

    fun filter(
        filter: (T) -> Boolean
    ): FeedBuilderBuilder<S, I, A, T> {
        feedBuilder.filter = filter
        return this
    }
}