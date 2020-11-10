package com.agile4j.feed.builder

import com.agile4j.feed.builder.mock.Article
import com.agile4j.feed.builder.mock.ArticleView
import com.agile4j.feed.builder.mock.articleMapper
import com.agile4j.feed.builder.mock.getArticleByIds
import com.agile4j.feed.builder.mock.getArticlesByTimeAsc
import org.junit.Assert
import org.junit.Test

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */
class TestByModelBuilder {

    private val feedBuilder = FeedBuilderFactory
        .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
        .searchCount{ 10 }
        .maxSearchCount{ 100 }
        .searchBufferSize{ 3 }
        .searchTimesLimit{ 5 }
        .maxSearchBatchSize{ 100 }
        //.topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L) }
        .topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L) }
        .fixedSupplier(FixedPosition.SECOND) { listOf(6L, 7L, 8L) }
        .builder(::getArticleByIds)
        .mapper(::articleMapper)
        .indexFilter { it > 0 }
        .batchIndexFilter { ids -> ids.associateWith { it > 0 } }
        .filter { it.id > 0 }
        .targetFilter { view -> view.article.id > 0 }
        .noMoreCursor("no_more")
        .build()

    @Test
    fun testByModelBuilder() {
        val response = feedBuilder.buildBy("")
        val articleViews: List<ArticleView> = response.list
        val nextCursor: String = response.nextCursor

        Assert.assertEquals(10, articleViews.size)
        Assert.assertTrue(nextCursor.startsWith("TAIL;11;11;"))
        articleViews.forEach{ println(it) }
        println(nextCursor)
    }

    @Test
    fun testNoMore() {
        val response = feedBuilder.buildBy("no_more")
        val articleViews: List<ArticleView> = response.list
        val nextCursor: String = response.nextCursor
        Assert.assertEquals(0, articleViews.size)
        Assert.assertTrue(nextCursor == "no_more")
    }
}