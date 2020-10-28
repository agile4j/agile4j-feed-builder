package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */
class Cursor {
    /**
     * 组成部分 值有四种情况[TOP]、[TAIL]、[NO_MORE]、[FixedPosition.name]
     */
    val positionStr: String = NO_MORE
    /**
     * 排序项 值不允许包含字符[CURSOR_SEPARATOR]、[INDEX_SEPARATOR]
     */
    val sortStr: String = ""

}