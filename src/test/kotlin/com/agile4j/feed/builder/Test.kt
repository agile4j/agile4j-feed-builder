package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */

fun main(args: Array<String>) {
    val i1: Int = 1
    val i2: Int = 1
    println(i1 == i2)

    val position = Position.ofName("23")
    println(position)

    val feedBuilder = FeedBuilderFactory
        .descLongBuilder<Article, ArticleView>(::getArticlesByTimeDesc)
        .searchCount(10)
        .maxSearchCount(100)
        .searchBufferSize(3)
        .searchTimesLimit(5)
        .maxSearchBatchSize(100)
        .topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L) }
        .fixedSupplier(FixedPosition.SECOND) { listOf(6L, 7L, 8L) }
        .builder(::getArticleByIds)
        .mapper(::articleMapper)
        .targetFilter { view -> view.article.id > 0 }
        .build()
    val response = feedBuilder.buildBy("")
    val articleViews = response.list
    val nextCursor = response.nextCursor

    println("abc")
    println(articleViews)
}

data class Article(val id: Long)

data class ArticleView(val article: Article)

fun getArticleByIds(ids: Collection<Long>): Map<Long, Article> {
    return emptyMap()
}

fun articleMapper(articles: Collection<Article>): Map<Article, ArticleView> {
    return emptyMap()
}

fun getArticlesByTimeDesc(timeFrom: Long, searchCount: Int): List<Pair<Long, Long>> {
    return emptyList()
}

class Model

fun getModelsByTimeDesc(timeFrom: Long, searchCount: Int): List<Model> {
    return emptyList()
}