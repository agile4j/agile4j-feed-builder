package com.agile4j.feed.builder

import org.apache.commons.lang3.math.NumberUtils

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */
object FeedBuilderFactory {

    /**
     * 适用于排序项、索引类型都为Long的降序feed
     */
    fun <A, T> descLongBuilder(
        supplier: (Long, Int) -> List<Long>
    ) = generalBuilder<Long, Long, A, T>(
        supplier,
        Long::toString, NumberUtils::toLong,
        Long::toString, NumberUtils::toLong,
        Long.MAX_VALUE, Long.MAX_VALUE)

    /**
     * 适用于排序项、索引类型都为Long的升序feed
     */
    fun <A, T> ascLongBuilder(
        supplier: (Long, Int) -> List<Long>
    ) = generalBuilder<Long, Long, A, T>(
        supplier,
        Long::toString, NumberUtils::toLong,
        Long::toString, NumberUtils::toLong,
        0L, 0L)

    /**
     * @param S sortType 排序项类型 例如时间戳对应Long
     * @param I indexType 索引类型 例如DB主键对应Long
     * @param A accompanyType 伴生资源类型 例如文章类Article
     * @param T targetType 映射目标类型 例如文章视图ArticleView
     *
     * @param supplier (sortFrom: SortType, searchCount: Int) -> List<IndexType>
     * @param sortEncoder 排序项编码器 encode后的值不允许包含字符[CURSOR_SEPARATOR]、[INDEX_SEPARATOR]
     * @param sortDecoder 排序项解码器
     * @param indexEncoder 索引编码器 encode后的值不允许包含字符[CURSOR_SEPARATOR]、[INDEX_SEPARATOR]
     * @param indexDecoder 索引解码器
     * @param sortInitValue 排序项初始值
     */
    fun <S, I, A, T> generalBuilder(
        supplier: (S, Int) -> List<I>,
        sortEncoder: (S) -> String,
        sortDecoder: (String) -> S,
        indexEncoder: (I) -> String,
        indexDecoder: (String) -> I,
        sortInitValue: S,
        indexInitValue: I
    ): FeedBuilderBuilder<S, I, A, T> {
        return FeedBuilderBuilder(
            supplier,
            sortEncoder, sortDecoder,
            indexEncoder, indexDecoder,
            sortInitValue,
            indexInitValue)
    }

}
