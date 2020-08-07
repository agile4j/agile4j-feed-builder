package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * Created on 2020-08-07
 */

fun main() {
    // 1. feed流组成 multiPart fixedPosition

    // 2. 各部分优先级

    // 3. fixedPosition 选取策略

    // 4. 排序

    // 5. 过滤

    // 6. 边界条件：是否把fixedPosition前移？不移，改为不展示

    // 7. 对feed流中元素的处理（嵌套结构的填充、groupId的注入）

    val cursor = ""

    val feeds = feedBuilder<Long, Any>().buildBy(cursor)
    println("abc")
}

fun <O, E> feedBuilder(): FeedBuilder<E> {
    return FeedBuilderFactory<O, E>()
        .getBuilder()
}

interface Retriever<O, E> {
    fun get(offset: O, limit: Int): List<E>
}

class FeedBuilderFactory<O, E> {

    val parts = LinkedHashMap<String, Retriever<O, E>>()

    fun partOf(partName: String, partRetriever: Retriever<O, E>): FeedBuilderFactory<O, E> {
        if (parts.keys.contains(partName)) else throw RuntimeException("redundant partName:$partName")
        parts[partName] = partRetriever
        return this
    }

    fun getBuilder(): FeedBuilder<E> {
        return FeedBuilder<E>()
    }
}

/**
 * 注意，非线程安全，请在线程安全环境下使用
 */
class FeedBuilder<E> {
    fun buildBy(cursor: String): List<E> {
        return emptyList()
    }
}

class Cursor {

}
