package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */

fun main(args: Array<String>) {
    val feedBuilder = FeedBuilderFactory
            .newBuilder<Long, Long, Article, ArticleView>(::getArticlesByTimeDesc)
            .searchCount(10)
            .searchBufferSize(3)
            .searchTimesLimit(5)
            .maxSearchBatchSize(100)
            .topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L) }
            //.fixedSupplier()
            .build()
    val articles = feedBuilder.buildBy("no_more")
    println("abc")
}

data class Article(val id: Long)

data class ArticleView(val article: Article)

fun getArticlesByTimeDesc(timeFrom: Long, searchCount: Int): List<Long> {
    return emptyList()
}