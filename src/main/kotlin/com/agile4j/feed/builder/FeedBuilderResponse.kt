package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * Created on 2020-10-29
 */
class FeedBuilderResponse<T>(val list: List<T>, val nextCursor: String) {

    companion object {
        fun <T> noMoreInstance() = FeedBuilderResponse<T>(
            emptyList(), NO_MORE_CURSOR_STR)
    }

}