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
        .fixedSupplier(Position.SECOND) { listOf(6L, 7L, 8L) }
        .builder(::getArticleByIds)
        .mapper(::articleMapper)
        .filter { view -> view.article.id > 0 }
        .build()
    val articleViews = feedBuilder.buildBy("no_more")

    val feedBuilder2 = FeedBuilderFactory
        .newBuilder<Long, Long, Long, Long>(::getArticlesByTimeDesc)
        .searchCount(10)
        .searchBufferSize(3)
        .searchTimesLimit(5)
        .maxSearchBatchSize(100)
        .topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L) }
        .fixedSupplier(Position.SECOND) { listOf(6L, 7L, 8L) }
        .builder{ indices -> indices.associateWith { it } }
        .mapper { accompanies -> accompanies }
        .build()
    println("abc")
}

data class Article(val id: Long)

data class ArticleView(val article: Article)

fun getArticleByIds(ids: Collection<Long>): Map<Long, Article> {
    return emptyMap()
}

fun articleMapper(articles: Collection<Article>): Collection<ArticleView> {
    return emptyList()
}

fun getArticlesByTimeDesc(timeFrom: Long, searchCount: Int): List<Long> {
    return emptyList()
}