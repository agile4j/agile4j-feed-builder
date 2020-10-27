package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */
object FeedBuilderFactory {

    /**
     * @param S sortType 排序项类型
     * @param R resourceType 资源类型
     * @param T targetType 映射目标类型
     * @param supplier (sortFrom: SortType, searchCount: Int) -> List<ResourceType>
     */
    fun <S, R, T> newBuilder(supplier: (S, Int) -> List<R>): FeedBuilderBuilder<S, R, T> {
        return FeedBuilderBuilder(supplier)
    }
}