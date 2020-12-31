package com.agile4j.feed.builder

import com.agile4j.feed.builder.mock.Article
import com.agile4j.feed.builder.mock.ArticleView
import com.agile4j.feed.builder.mock.getArticleByIds
import com.agile4j.feed.builder.mock.getArticlesByTimeAsc
import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.indexBy
import org.junit.Assert
import org.junit.Test

/**
 * @author liurenpeng
 * Created on 2020-11-02
 */
class TestByBuilderAndMapper {

    private val feedBuilder = getBuilderBuilder().build()

    private fun getBuilderBuilder(): FeedBuilderBuilder<Long, Long, Article, ArticleView> {
        return FeedBuilderFactory
            .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
            .searchCount{ 10 }
            .maxSearchCount{ 100 }
            .searchBufferSize{ 3 }
            .searchTimesLimit{ 5 }
            .maxSearchBatchSize{ 100 }
            //.topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L) }
            .topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L) }
            .fixedSupplier(FixedPosition.SECOND) { listOf(6L, 7L, 8L) }
            .indexFilter { it > 0 }
            .batchIndexFilter { ids -> ids.associateWith { it > 0 } }
            .filter { it.id > 0 }
            .targetFilter { view -> view.article.id > 0 }
    }

    private fun initModelBuilder() {
        Article::class indexBy Article::id
        Article::class buildBy ::getArticleByIds

        ArticleView::class accompanyBy Article::class
    }

    @Test
    fun testByBuilderAndMapper() {
        initModelBuilder()

        val response = feedBuilder.buildBy("")
        val articleViews: List<ArticleView> = response.list
        val nextCursor: String = response.nextCursor

        Assert.assertEquals(10, articleViews.size)
        Assert.assertTrue(nextCursor.startsWith("TAIL;11;11;"))
        articleViews.forEach{ println(it) }
        println(nextCursor)
    }

    @Test
    fun testIndexOutOfBounds() {
        initModelBuilder()

        val response = getBuilderBuilder()
            .maxSearchCount { Int.MAX_VALUE }
            .build()
            .buildBy("", 1000)
        val articleViews: List<ArticleView> = response.list
        val nextCursor: String = response.nextCursor

        println("size:${articleViews.size} nextCursor:$nextCursor")
        Assert.assertEquals(1000, articleViews.size)
        Assert.assertEquals("no_more", nextCursor)
    }
}
