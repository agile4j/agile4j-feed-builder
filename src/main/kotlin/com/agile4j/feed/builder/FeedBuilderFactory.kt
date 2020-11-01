package com.agile4j.feed.builder

import org.apache.commons.lang3.math.NumberUtils
import java.util.Comparator.comparingLong
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * @date Created in 20-10-27
 */
object FeedBuilderFactory {

    /**
     * 适用于排序项、索引类型都为Long的降序feed
     */
    fun <A: Any, T: Any> descLongBuilder(
        accompanyClass: Class<A>,
        targetClass: Class<T>,
        supplier: (Long, Int) -> List<Pair<Long, Long>>
    ) = descLongBuilder(
        accompanyClass.kotlin, targetClass.kotlin, supplier)

    /**
     * 适用于排序项、索引类型都为Long的降序feed
     */
    fun <A: Any, T: Any> descLongBuilder(
        accompanyClass: KClass<A>,
        targetClass: KClass<T>,
        supplier: (Long, Int) -> List<Pair<Long, Long>>
    ) = generalBuilder<Long, Long, A, T>(
        Long::class, accompanyClass, targetClass, supplier,
        Long::toString, NumberUtils::toLong,
        Long::toString, NumberUtils::toLong,
        Long.MAX_VALUE, Long.MAX_VALUE,
        comparingLong {it}, comparingLong {it},
        SortType.DESC)

    /**
     * 适用于排序项、索引类型都为Long的升序feed
     */
    fun <A: Any, T: Any> ascLongBuilder(
        accompanyClass: Class<A>,
        targetClass: Class<T>,
        supplier: (Long, Int) -> List<Pair<Long, Long>>
    ) = ascLongBuilder(
        accompanyClass.kotlin, targetClass.kotlin, supplier)

    /**
     * 适用于排序项、索引类型都为Long的升序feed
     */
    fun <A: Any, T: Any> ascLongBuilder(
        accompanyClass: KClass<A>,
        targetClass: KClass<T>,
        supplier: (Long, Int) -> List<Pair<Long, Long>>
    ) = generalBuilder<Long, Long, A, T>(
        Long::class, accompanyClass, targetClass, supplier,
        Long::toString, NumberUtils::toLong,
        Long::toString, NumberUtils::toLong,
        0L, 0L,
        comparingLong {it}, comparingLong {it},
        SortType.ASC)

    /**
     * TODO sortTyp已经限定为Number子类了，关于sort的function是不是可以简化掉
     *
     * @param S sortType 排序项类型 例如时间戳对应Long 必须是以下类型之一：
     * [Double]、[Float]、[Long]、[Int]、[Short]、[Byte]
     * @param I indexType 索引类型 例如DB主键对应Long
     * @param A accompanyType 伴生资源类型 例如文章类Article
     * @param T targetType 映射目标类型 例如文章视图ArticleView
     *
     * @param supplier (sortFrom: SortType, searchCount: Int) -> List<IndexType>
     * @param sortEncoder 排序项编码器 encode后的值不允许包含字符[CURSOR_SEPARATOR]、[INDEX_SEPARATOR]
     * @param sortDecoder 排序项解码器
     * @param indexEncoder 索引编码器 encode后的值不允许包含字符[CURSOR_SEPARATOR]、[INDEX_SEPARATOR]
     * @param indexDecoder 索引解码器
     * @param sortInitValue 排序项初始值
     */
    fun <S: Number, I: Any, A: Any, T: Any> generalBuilder(
        indexClass: KClass<I>,
        accompanyClass: KClass<A>,
        targetClass: KClass<T>,
        supplier: (S, Int) -> List<Pair<I, S>>,
        sortEncoder: (S) -> String,
        sortDecoder: (String) -> S,
        indexEncoder: (I) -> String,
        indexDecoder: (String) -> I,
        sortInitValue: S,
        indexInitValue: I,
        sortComparator: Comparator<S>,
        indexComparator: Comparator<I>,
        sortType: SortType
    ): FeedBuilderBuilder<S, I, A, T> {
        return FeedBuilderBuilder(
            indexClass, accompanyClass, targetClass,
            supplier,
            sortEncoder, sortDecoder,
            indexEncoder, indexDecoder,
            sortInitValue,
            indexInitValue,
            sortComparator,
            indexComparator,
            sortType)
    }

}
