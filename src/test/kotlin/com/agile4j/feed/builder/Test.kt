package com.agile4j.feed.builder

import com.agile4j.feed.builder.mock.Article
import com.agile4j.feed.builder.mock.ArticleView
import com.agile4j.feed.builder.mock.articleMapper
import com.agile4j.feed.builder.mock.getArticleByIds
import com.agile4j.feed.builder.mock.getArticlesByTimeAsc

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */

val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .searchCount{ 10 }
    .maxSearchCount{ 100 }
    .searchBufferSize{ 3 }
    .searchTimesLimit{ 5 }
    .maxSearchBatchSize{ 100 }
    .topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L) }
    //.topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L) }
    .fixedSupplier(FixedPosition.SECOND) { listOf(6L, 7L, 8L) }
    .builder(::getArticleByIds)
    .mapper(::articleMapper)
    .indexFilter { it > 0 }
    .batchIndexFilter { ids -> ids.associateWith { it > 0 } }
    .filter { it.id > 0 }
    .targetFilter { view -> view.article.id > 0 }
    .build()

fun main(args: Array<String>) {
    val result = getArticlesByTimeAsc(-100L, 10)
    println(result)


    val response = feedBuilder.buildBy("TOP;-9223372036854775808;11;6")
    val articleViews = response.list
    val nextCursor = response.nextCursor

    articleViews.forEach{ println(it) }
    println(nextCursor)
}