package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */
class Cursor<S, I>(
    /**
     * 所在feed组成部分的位置
     */
    val position: Position,
    /**
     * 排序项 encode后的值不允许包含字符[CURSOR_SEPARATOR]、[INDEX_SEPARATOR]
     */
    val sort: S,
    /**
     * 索引项 值不允许包含字符[CURSOR_SEPARATOR]、[INDEX_SEPARATOR]
     */
    val index: I,
    /**
     * 已曝光随机索引集
     */
    val showedRandomIndices: MutableSet<I>
) {

    companion object {
        /**
         * @param cursorStr 必须是符合格式的值，否则抛出[CheckException]
         * 格式示例：TOP;2492;96789002;90009623,32397452,94994452
         * 格式要求：
         * 1. notBlank
         * 2. 有且只有3个[CURSOR_SEPARATOR]
         * 3. sort、index，可通过相应的encoder、decoder正常编解码
         */
        /*fun <S, I> of(cursorStr: String): Cursor<S, I> {

        }*/
    }
}