package com.agile4j.feed.builder.mock

/**
 * @author liurenpeng
 * Created on 2020-11-01
 */

const val idBorder = 1000L

val allArticles = (1..idBorder).toList().map { it to Article(it) }.toMap()

fun articleIdToTime(): LinkedHashMap<Long, Long> {
    val map = LinkedHashMap<Long, Long>()
    map.putAll((1L..20).associateWith { it })
    map.putAll((21L..30).associateWith { 21L })
    map.putAll((31L..60).associateWith { it })
    map.putAll((61L..99).associateWith { 61L })
    map.putAll((100..idBorder).associateWith { it })
    return map
}