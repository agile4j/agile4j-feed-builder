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
        supplier: (Long, Int) -> LinkedHashMap<Long, Long>
    ) = descLongBuilderEx(
        accompanyClass.kotlin, targetClass.kotlin){ sort, limit ->
        supplier.invoke(sort, limit).entries.map { it.key to it.value }}

    /**
     * 适用于排序项、索引类型都为Long的降序feed
     */
    fun <A: Any, T: Any> descLongBuilder(
        accompanyClass: KClass<A>,
        targetClass: KClass<T>,
        supplier: (Long, Int) -> LinkedHashMap<Long, Long>
    ) = descLongBuilderEx(
        accompanyClass, targetClass){ sort, limit ->
        supplier.invoke(sort, limit).entries.map { it.key to it.value }}

    /**
     * 适用于排序项、索引类型都为Long的降序feed
     */
    fun <A: Any, T: Any> descLongBuilderEx(
        accompanyClass: Class<A>,
        targetClass: Class<T>,
        supplier: (Long, Int) -> List<Pair<Long, Long>>
    ) = descLongBuilderEx(
        accompanyClass.kotlin, targetClass.kotlin, supplier)

    /**
     * 适用于排序项、索引类型都为Long的降序feed
     */
    fun <A: Any, T: Any> descLongBuilderEx(
        accompanyClass: KClass<A>,
        targetClass: KClass<T>,
        supplier: (Long, Int) -> List<Pair<Long, Long>>
    ) = generalBuilder(
        Long::class, Long::class, accompanyClass, targetClass, supplier,
        Long::toString, NumberUtils::toLong,
        { Long.MAX_VALUE }, comparingLong {it}, SortType.DESC)

    /**
     * 适用于排序项、索引类型都为Long的升序feed
     */
    fun <A: Any, T: Any> ascLongBuilder(
        accompanyClass: Class<A>,
        targetClass: Class<T>,
        supplier: (Long, Int) -> LinkedHashMap<Long, Long>
    ) = ascLongBuilderEx(
        accompanyClass.kotlin, targetClass.kotlin){ sort, limit ->
        supplier.invoke(sort, limit).entries.map { it.key to it.value }}

    /**
     * 适用于排序项、索引类型都为Long的升序feed
     */
    fun <A: Any, T: Any> ascLongBuilder(
        accompanyClass: KClass<A>,
        targetClass: KClass<T>,
        supplier: (Long, Int) -> LinkedHashMap<Long, Long>
    ) = ascLongBuilderEx(
        accompanyClass, targetClass){ sort, limit ->
        supplier.invoke(sort, limit).entries.map { it.key to it.value }}

    /**
     * 适用于排序项、索引类型都为Long的升序feed
     */
    fun <A: Any, T: Any> ascLongBuilderEx(
        accompanyClass: Class<A>,
        targetClass: Class<T>,
        supplier: (Long, Int) -> List<Pair<Long, Long>>
    ) = ascLongBuilderEx(
        accompanyClass.kotlin, targetClass.kotlin, supplier)

    /**
     * 适用于排序项、索引类型都为Long的升序feed
     */
    fun <A: Any, T: Any> ascLongBuilderEx(
        accompanyClass: KClass<A>,
        targetClass: KClass<T>,
        supplier: (Long, Int) -> List<Pair<Long, Long>>
    ) = generalBuilder(
        Long::class, Long::class, accompanyClass, targetClass, supplier,
        Long::toString, NumberUtils::toLong,
        { Long.MIN_VALUE }, comparingLong {it}, SortType.ASC)

    fun <S: Number, I: Any, A: Any, T: Any> generalBuilder(
        sortClass: Class<S>,
        indexClass: Class<I>,
        accompanyClass: Class<A>,
        targetClass: Class<T>,
        supplier: (S, Int) -> List<Pair<I, S>>,
        indexEncoder: (I) -> String,
        indexDecoder: (String) -> I,
        indexInitValue: () -> I,
        indexComparator: Comparator<I>,
        sortType: SortType
    ) = generalBuilder(
        sortClass.kotlin, indexClass.kotlin,
        accompanyClass.kotlin, targetClass.kotlin,
        supplier,
        indexEncoder, indexDecoder,
        indexInitValue, indexComparator,
        sortType)

    /**
     * @param sortClass 排序项类型 必须是以下类型之一：[Double]、[Float]、[Long]、[Int]、[Short]、[Byte]
     * @param indexClass 索引类型 例如DB主键一般对应[Long]
     * @param accompanyClass 伴生资源类型 例如文章类Article
     * @param targetClass 映射目标类型 例如文章视图ArticleView
     * @param supplier (sortFrom: S, searchCount: Int) -> List<Pair<I, S>>
     * @param indexEncoder 索引编码器 encode后的值不允许包含字符[CURSOR_SEPARATOR]、[INDEX_SEPARATOR]
     * @param indexDecoder 索引解码器
     * @param indexInitValue 索引初始值 请求第一页数据时按改值初始化
     * @param indexComparator 索引比较器
     * @param sortType 排序类型 可选项：[SortType.DESC]、[SortType.ASC]
     */
    fun <S: Number, I: Any, A: Any, T: Any> generalBuilder(
        sortClass: KClass<S>,
        indexClass: KClass<I>,
        accompanyClass: KClass<A>,
        targetClass: KClass<T>,
        supplier: (S, Int) -> List<Pair<I, S>>,
        indexEncoder: (I) -> String,
        indexDecoder: (String) -> I,
        indexInitValue: () -> I,
        indexComparator: Comparator<I>,
        sortType: SortType
    ): FeedBuilderBuilder<S, I, A, T> {
        return FeedBuilderBuilder(
            sortClass, indexClass, accompanyClass, targetClass, supplier,
            indexEncoder, indexDecoder, indexInitValue, indexComparator, sortType)
    }

}
