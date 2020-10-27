package com.agile4j.feed.builder

/**
 * @param S sortType 排序项类型
 * @param R resourceType 资源类型
 * @param T targetType 映射目标类型
 * @author liurenpeng
 * Created on 2020-08-07
 */
class FeedBuilder<S, R, T>(internal val supplier: (S, Int) -> List<R>) {

    internal var searchCount: Int = DEFAULT_SEARCH_COUNT
    internal var searchBufferSize: Int = DEFAULT_SEARCH_BUFFER_SIZE
    internal var searchTimesLimit: Int = DEFAULT_SEARCH_TIMES_LIMIT
    internal var maxSearchBatchSize: Int = DEFAULT_MAX_SEARCH_BATCH_SIZE

    fun buildBy(cursorStr: String): List<R> {
        return emptyList()
    }
}


