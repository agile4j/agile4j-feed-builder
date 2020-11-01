package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */
class FeedBuilderCursor<S: Number, I: Any>(
    /**
     * 所在feed组成部分的位置
     */
    val position: Position,
    /**
     * 排序项
     */
    val sort: S,
    /**
     * 索引项
     */
    val index: I,
    /**
     * 已曝光随机索引集
     */
    val showedRandomIndices: MutableSet<I>,
    /**
     * 是否第一次请求
     */
    val isFirstPage: Boolean
) {
    fun isNoMore() = position == Position.NO_MORE
    fun isTail() = position == Position.TAIL
}