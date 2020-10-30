package com.agile4j.feed.builder

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */

const val NO_MORE_CURSOR_STR = "NO_MORE;;;"

const val CURSOR_SEPARATOR = ";"
const val INDEX_SEPARATOR = ","

// 每次获取的资源条数
const val DEFAULT_SEARCH_COUNT = 10

// 每次获取的最大资源条数
const val DEFAULT_MAX_SEARCH_COUNT = Int.MAX_VALUE

// 为避免读时过滤导致多次查询增加的额外查询条数
const val DEFAULT_SEARCH_BUFFER_SIZE = 3

// 为避免死循环限制最多循环获取资源次数
const val DEFAULT_SEARCH_TIMES_LIMIT = 5

// 当排序项相同的资源条量大于“每次获取的资源条数”时，一次性把该排序项对应值下的资源全部取出时的limit大数值
const val DEFAULT_MAX_SEARCH_BATCH_SIZE = 100

val JSON_MAPPER = ObjectMapper().registerKotlinModule()

enum class SortType {
    ASC, DESC
}

enum class FixedPosition(val number: Int) {
    FIRST(1),
    SECOND(2),
    THIRD(3),
    FOURTH(4),
    FIFTH(5),
    SIXTH(6),
    SEVENTH(7),
    EIGHTH(8),
    NINTH(9),
    TENTH(10),
    P11TH(11),
    P12TH(12),
    P13TH(13),
    P14TH(14),
    P15TH(15),
    P16TH(16),
    P17TH(17),
    P18TH(18),
    P19TH(19),
    P20TH(20),
}

enum class Position(val fixedPosition: FixedPosition?) {
    TOP(null),
    TAIL(null),
    NO_MORE(null),

    FIRST(FixedPosition.FIRST),
    SECOND(FixedPosition.SECOND),
    THIRD(FixedPosition.THIRD),
    FOURTH(FixedPosition.FOURTH),
    FIFTH(FixedPosition.FIFTH),
    SIXTH(FixedPosition.SIXTH),
    SEVENTH(FixedPosition.SEVENTH),
    EIGHTH(FixedPosition.EIGHTH),
    NINTH(FixedPosition.NINTH),
    TENTH(FixedPosition.TENTH),
    P11TH(FixedPosition.P11TH),
    P12TH(FixedPosition.P12TH),
    P13TH(FixedPosition.P13TH),
    P14TH(FixedPosition.P14TH),
    P15TH(FixedPosition.P15TH),
    P16TH(FixedPosition.P16TH),
    P17TH(FixedPosition.P17TH),
    P18TH(FixedPosition.P18TH),
    P19TH(FixedPosition.P19TH),
    P20TH(FixedPosition.P20TH);

    fun isFixed(): Boolean = fixedPosition != null

    companion object {
        fun ofName(name: String): Position? {
            return try {
                valueOf(name)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}