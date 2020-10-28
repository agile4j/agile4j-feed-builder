package com.agile4j.feed.builder

/**
 * @param S sortType 排序项类型
 * @param I indexType 索引类型
 * @param A accompanyType 伴生资源类型
 * @param T targetType 映射目标类型
 * @author liurenpeng
 * Created on 2020-08-07
 */
class FeedBuilder<S, I, A, T>(internal val supplier: (S, Int) -> List<I>) {

    internal var searchCount: Int = DEFAULT_SEARCH_COUNT
    internal var searchBufferSize: Int = DEFAULT_SEARCH_BUFFER_SIZE
    internal var searchTimesLimit: Int = DEFAULT_SEARCH_TIMES_LIMIT
    internal var maxSearchBatchSize: Int = DEFAULT_MAX_SEARCH_BATCH_SIZE
    internal var topNSupplier: () -> List<I> = ::emptyList
    internal var fixedSupplierMap: MutableMap<Position, () -> List<I>> = mutableMapOf()
    internal var builder: ((Collection<I>) -> Map<I, A>)? = null
    internal var mapper: ((Collection<A>) -> Collection<T>)? = null
    internal var filter: (T) -> Boolean = { true }

    fun buildBy(cursorStr: String): List<T> {
        return emptyList()
    }
    fun buildBy(cursorStr: String, searchCount: Int): List<A> {
        // 校验：
        // 1. cursor格式
        // 2. searchCount大于等于fixedSupplierMap key的最大值
        return emptyList()
    }

}


