package com.agile4j.feed.builder

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
    private val indexDecoder: (String) -> I

) {

    fun buildBy(cursorStr: String): List<T> = buildBy(cursorStr, searchCount)

    fun buildBy(cursorStr: String, searchCount: Int): List<T> {
        // TODO 校验：
        // 1. cursor格式
        // 2. searchCount大于等于fixedSupplierMap key的最大值
        return emptyList()
    }

}


