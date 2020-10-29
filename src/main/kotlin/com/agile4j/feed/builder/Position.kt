package com.agile4j.feed.builder

/**
 * @author liurenpeng
 * Created on 2020-10-29
 */
enum class Position(val fixedPosition: FixedPosition?) {
    TOP(null),
    TAIL(null),
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
}