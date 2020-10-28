package com.agile4j.feed.builder

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */

const val TOP = "TOP"
const val TAIL = "TAIL"
const val NO_MORE = "NO_MORE"

const val CURSOR_SEPARATOR = ";"
const val INDEX_SEPARATOR = ","

// 每次获取的资源条数
const val DEFAULT_SEARCH_COUNT = 10

// 为避免读时过滤导致多次查询增加的额外查询条数
const val DEFAULT_SEARCH_BUFFER_SIZE = 3

// 为避免死循环限制一次请求最多获取资源次数
const val DEFAULT_SEARCH_TIMES_LIMIT = 5

// 当排序项相同的资源条量大于“每次获取的资源条数”时，一次性把该排序项对应值下的资源全部取出时的limit大数值
const val DEFAULT_MAX_SEARCH_BATCH_SIZE = 100

val JSON_MAPPER = ObjectMapper().registerKotlinModule()