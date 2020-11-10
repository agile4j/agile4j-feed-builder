package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * Created on 2020-10-29
 */
class FeedBuilderResponse<T: Any>(val list: List<T>, val nextCursor: String) {

    companion object {
        fun <T: Any> noMoreResp(noMoreCursor: String) =
            noMoreResp<T>(emptyList(), noMoreCursor)
        fun <T: Any> noMoreResp(list: List<T>, noMoreCursor: String) =
            FeedBuilderResponse(list, noMoreCursor)
    }

}