package com.agile4j.feed.builder

/**
 * @param S sortType 排序项类型
 * @param R resourceType 资源类型
 * @param T targetType 映射目标类型
 * @author liurenpeng
 * @date Created in 20-10-27
 */
class FeedBuilderBuilder<S, R, T>(supplier: (S, Int) -> List<R>) {

    private val feedBuilder: FeedBuilder<S, R, T> = FeedBuilder(supplier)

    fun build(): FeedBuilder<S, R, T> {
        return feedBuilder
    }

    /**
     * 每次获取的资源条数
     * 默认值[DEFAULT_SEARCH_COUNT]
     */
    fun searchCount(searchCount: Int): FeedBuilderBuilder<S, R, T> {
        feedBuilder.searchCount = searchCount
        return this
    }

    /**
     * 为避免读时过滤导致多次查询增加的额外查询条数
     * 默认值[DEFAULT_SEARCH_BUFFER_SIZE]
     */
    fun searchBufferSize(searchBufferSize: Int): FeedBuilderBuilder<S, R, T> {
        feedBuilder.searchBufferSize = searchBufferSize
        return this
    }

    /**
     * 为避免死循环限制一次请求最多获取资源次数
     * 默认值[DEFAULT_SEARCH_TIMES_LIMIT]
     */
    fun searchTimesLimit(searchTimesLimit: Int): FeedBuilderBuilder<S, R, T> {
        feedBuilder.searchTimesLimit = searchTimesLimit
        return this
    }

    /**
     * 当排序项相同的资源条量大于“每次获取的资源条数”时，一次性把该排序项对应值下的资源全部取出时的limit大数值
     * 默认值[DEFAULT_MAX_SEARCH_BATCH_SIZE]
     */
    fun maxSearchBatchSize(maxSearchBatchSize: Int): FeedBuilderBuilder<S, R, T> {
        feedBuilder.maxSearchBatchSize = maxSearchBatchSize
        return this
    }

}