package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * Created on 2020-10-29
 */
class FeedBuilderResponse<T>(val result: List<T>, val nextCursor: String)