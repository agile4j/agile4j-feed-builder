package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */
object FeedBuilderFactory {

    /**
     * @param S sortType 排序项类型 例如时间戳对应Long
     * @param I indexType 索引类型 例如DB主键对应Long
     * @param A accompanyType 伴生资源类型 例如文章类Article
     * @param T targetType 映射目标类型 例如文章视图ArticleView
     * @param supplier (sortFrom: SortType, searchCount: Int) -> List<IndexType>
     */
    fun <S, I, A, T> newBuilder(supplier: (S, Int) -> List<I>): FeedBuilderBuilder<S, I, A, T> {
        return FeedBuilderBuilder(supplier)
    }
}