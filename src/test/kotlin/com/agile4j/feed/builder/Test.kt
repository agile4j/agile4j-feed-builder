package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */

fun main(args: Array<String>) {
    val feedBuilder = FeedBuilderFactory
        .descLongBuilder(Article::class, ArticleView::class, ::getArticlesByTimeDesc)
        .searchCount{ 10 }
        .maxSearchCount{ 100 }
        .searchBufferSize{ 3 }
        .searchTimesLimit{ 5 }
        .maxSearchBatchSize{ 100 }
        .topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L) }
        .fixedSupplier(FixedPosition.SECOND) { listOf(6L, 7L, 8L) }
        .builder(::getArticleByIds)
        .mapper(::articleMapper)
        .indexFilter { it > 0 }
        .batchIndexFilter { ids -> ids.associateWith { it > 0 } }
        .filter { it.id > 0 }
        .targetFilter { view -> view.article.id > 0 }
        .build()
    val response = feedBuilder.buildBy("")
    val articleViews = response.list
    val nextCursor = response.nextCursor

    println(articleViews)
    println(nextCursor)
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