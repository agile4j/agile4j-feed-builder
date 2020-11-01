package com.agile4j.feed.builder.mock

import java.util.concurrent.atomic.AtomicInteger

/**
 * @author liurenpeng
 * Created on 2020-11-01
 */

fun getArticleByIds(ids: Collection<Long>): Map<Long, Article> {
    accessTimes.incrementAndGet()
    println("===getArticleByIds ids:$ids")
    return allArticles.filter { ids.contains(it.key) }
}

fun articleMapper(articles: Collection<Article>): Map<Article, ArticleView> {
    return articles.associateWith { ArticleView(it) }
}

fun getArticlesByTimeAsc(timeFrom: Long, searchCount: Int): List<Pair<Long, Long>> {
    val result = mutableListOf<Pair<Long, Long>>()
    for (idToTime in articleIdToTime()) {
        if (result.size == searchCount) return result
        if (idToTime.value < timeFrom) continue
        result.add(idToTime.key to idToTime.value)
    }
    return result
}

val accessTimes = AtomicInteger(0)