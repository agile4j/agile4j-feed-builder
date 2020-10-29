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
    private val indexDecoder: (String) -> I,
    private val sortInitValue: S,
    private val indexInitValue: I

) {

    fun buildBy(cursorStr: String?): FeedBuilderResponse<T> = buildBy(cursorStr, searchCount)

    /**
     * @param cursorStr 第一次请求传入""，后续请求透传上次请求的[FeedBuilderResponse.nextCursor]
     * @param searchCount 本次查询条数 优先级为：
     * 1. 当前参数传入的值
     * 2. 通过[FeedBuilderBuilder.searchCount]API指定的值
     * 3. 默认值[DEFAULT_SEARCH_COUNT]
     */
    fun buildBy(cursorStr: String?, searchCount: Int): FeedBuilderResponse<T> {
        // TODO 校验：
        // 1. cursor格式
        // 2. searchCount大于等于fixedSupplierMap key的最大值
        return FeedBuilderResponse(emptyList(), NO_MORE)
    }

    private fun buildInitCursor(): Cursor<S, I> = Cursor(
        Position.TOP, sortInitValue, indexInitValue, mutableSetOf())

    /*private fun encodeCursor(cursor: Cursor<S, I>): String {

    }

    private fun decodeCursor(cursorStr: String?): Cursor<S, I> {
        if (StringUtils.isBlank(cursorStr)) {
            return buildInitCursor()
        }
    }*/
}


