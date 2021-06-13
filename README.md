# agile4j-feed-builder
agile4j-feed-builder是用Kotlin语言实现的feed流构建器，可在Kotlin/Java工程中使用。

# 目录
   * [如何引入](#如何引入)
   * [解决的问题域](#解决的问题域)
      * [多组成部分feed流](#多组成部分feed流)
      * [优先级和去重问题](#优先级和去重问题)
      * [选取策略](#选取策略)
      * [排序项相同的问题](#排序项相同的问题)
      * [数量处理](#数量处理)
      * [读时过滤](#读时过滤)
   * [API](#API)
      * [构建feedBuilder](#构建feedBuilder)
         * [构建排序项类型和索引类型都为Long的降序feed流](#构建排序项类型和索引类型都为Long的降序feed流)
         * [构建排序项类型和索引类型都为Long的升序feed流](#构建排序项类型和索引类型都为Long的升序feed流)
         * [通用feed流API](#通用feed流API)
      * [构建feed](#构建feed)
      * [自定义参数](#自定义参数)
         * [每次获取的资源条数](#每次获取的资源条数)
         * [每次获取的最大资源条数](#每次获取的最大资源条数)
         * [为避免读时过滤导致多次查询增加的额外查询条数](#为避免读时过滤导致多次查询增加的额外查询条数)
         * [为避免耗时过长限制一次构建最多获取资源次数](#为避免耗时过长限制一次构建最多获取资源次数)
         * [limit大数值](#limit大数值)
         * [topN资源](#topN资源)
         * [固定位置资源](#固定位置资源)
         * [资源构建器](#资源构建器)
         * [资源映射器](#资源映射器)
         * [索引过滤器](#索引过滤器)
         * [批量索引过滤器](#批量索引过滤器)
         * [伴生资源过滤器](#伴生资源过滤器)
         * [映射目标过滤器](#映射目标过滤器)
   * [高级特性](#高级特性)
      * [集成agile4j-model-builder](#集成agile4j-model-builder)

# 如何引入

>Gradle
```groovy
dependencies {
    compile "com.agile4j:agile4j-feed-builder:1.1.3"
}
```
>Maven
```xml
<dependency>
    <groupId>com.agile4j</groupId>
    <artifactId>agile4j-feed-builder</artifactId>
    <version>1.1.3</version>
</dependency>
```

# 解决的问题域
* 用于解决通过cursor拉取feed流的场景。

## 多组成部分feed流
* feed流是由多部分组成的，组成分3类：
    1. 头部：例如 topN由运营配置
    2. 固定位：例如 置顶资源、第X固定位由运营配置
    3. 尾部：从默认池中取
* agile4j-feed-builder关于固定位置有一个限制：位置不可超出每次查询的记录条数。例如一次获取20条数据，则最大可固定位置为第20位。这样限制的目的是，降低系统复杂度。而且该设定基本可以满足正常的业务场景。

## 优先级和去重问题
* 一个资源在feed流的多个组成中出现，需要定义优先级，需要进行去重处理。
* agile4j-feed-builder优先级为：固定位>头部>尾部，固定位之间位置靠前的优先级高。不支持自定义优先级。

## 选取策略
* 策略分两类：
    1. 全选：默认策略，所有内容全部曝光
    2. 随机选1：从内容中随机选择1个曝光

## 排序项相同的问题
* 排序项：假设feed流为按稿件发布时间倒排的稿件列表，则排序项为稿件发布时间。
* 排序项相同时，希望多次访问feed，资源排序一致。
* 相同的资源数量过多，一次limit取不完，agile4j-feed-builder处理方案：
    * 下次取值时，一次性把该排序项对应值下的资源全部取出（通过limit大数实现，limit大数的值支持自定义，不保证真的能全部取出）。下下次取值时，将排序项手动-1(desc)/+1(asc)。

## 数量处理
* 数量相关的通用处理：入参的校正、查询时buffer的设置等等。

## 读时过滤
* 渲染后，根据资源属性判断是否可以曝光。

# API

## 构建feedBuilder

### 构建排序项类型和索引类型都为Long的降序feed流
* 根据accompanyClass、targetClass、supplier参数类型的不同，分为4个API：
```Kotlin
/**
 * 适用于排序项、索引类型都为Long的降序feed
 */
fun <A: Any, T: Any> descLongBuilder(
    accompanyClass: Class<A>,
    targetClass: Class<T>,
    supplier: (Long, Int) -> LinkedHashMap<Long, Long>
)

/**
 * 适用于排序项、索引类型都为Long的降序feed
 */
fun <A: Any, T: Any> descLongBuilder(
    accompanyClass: KClass<A>,
    targetClass: KClass<T>,
    supplier: (Long, Int) -> LinkedHashMap<Long, Long>
)

/**
 * 适用于排序项、索引类型都为Long的降序feed
 */
fun <A: Any, T: Any> descLongBuilderEx(
    accompanyClass: Class<A>,
    targetClass: Class<T>,
    supplier: (Long, Int) -> List<Pair<Long, Long>>
)

/**
 * 适用于排序项、索引类型都为Long的降序feed
 */
fun <A: Any, T: Any> descLongBuilderEx(
    accompanyClass: KClass<A>,
    targetClass: KClass<T>,
    supplier: (Long, Int) -> List<Pair<Long, Long>>
)
```

* 使用示例：
    * 若[集成agile4j-model-builder](#集成agile4j-model-builder)，可不进行[builder](#资源构建器)、[mapper](#资源映射器)声明
```Kotlin
val feedBuilder = FeedBuilderFactory
    .descLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeDesc)
    .builder(::getArticleByIds)
    .mapper(::articleMapper)
    .build()
    
// supplier：(sortFrom: S, searchCount: Int) -> List<Pair<I, S>>
fun getArticlesByTimeDesc(timeFrom: Long, searchCount: Int): List<Pair<Long, Long>>
// builder
fun getArticleByIds(ids: Collection<Long>): Map<Long, Article>
// mapper
fun articleMapper(articles: Collection<Article>): Map<Article, ArticleView>
```

### 构建排序项类型和索引类型都为Long的升序feed流
* 根据accompanyClass、targetClass、supplier参数类型的不同，分为4个API：
```Kotlin
/**
 * 适用于排序项、索引类型都为Long的升序feed
 */
fun <A: Any, T: Any> ascLongBuilder(
    accompanyClass: Class<A>,
    targetClass: Class<T>,
    supplier: (Long, Int) -> LinkedHashMap<Long, Long>
)

/**
 * 适用于排序项、索引类型都为Long的升序feed
 */
fun <A: Any, T: Any> ascLongBuilder(
    accompanyClass: KClass<A>,
    targetClass: KClass<T>,
    supplier: (Long, Int) -> LinkedHashMap<Long, Long>
)

/**
 * 适用于排序项、索引类型都为Long的升序feed
 */
fun <A: Any, T: Any> ascLongBuilderEx(
    accompanyClass: Class<A>,
    targetClass: Class<T>,
    supplier: (Long, Int) -> List<Pair<Long, Long>>
)

/**
 * 适用于排序项、索引类型都为Long的升序feed
 */
fun <A: Any, T: Any> ascLongBuilderEx(
    accompanyClass: KClass<A>,
    targetClass: KClass<T>,
    supplier: (Long, Int) -> List<Pair<Long, Long>>
)
```

* 使用示例：
    * 若[集成agile4j-model-builder](#集成agile4j-model-builder)，可不进行[builder](#资源构建器)、[mapper](#资源映射器)声明
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .builder(::getArticleByIds)
    .mapper(::articleMapper)
    .build()
    
// supplier：(sortFrom: S, searchCount: Int) -> List<Pair<I, S>>
fun getArticlesByTimeAsc(timeFrom: Long, searchCount: Int): List<Pair<Long, Long>>
// builder
fun getArticleByIds(ids: Collection<Long>): Map<Long, Article>
// mapper
fun articleMapper(articles: Collection<Article>): Map<Article, ArticleView>
```

### 通用feed流API
* 构建排序项类型为Number子类的，通用的feed流，根据class(KClass/Class)类型分为两个API：：
```Kotlin
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
)

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
)
```

* 使用示例：
    * 若[集成agile4j-model-builder](#集成agile4j-model-builder)，可不进行[builder](#资源构建器)、[mapper](#资源映射器)声明
```Kotlin
val feedBuilder = FeedBuilderFactory
    .generalBuilder(
            Long::class, Long::class, 
            Article::class, ArticleView::class, 
            ::getArticlesByTimeAsc,
            Long::toString, NumberUtils::toLong,
            { Long.MIN_VALUE }, comparingLong {it}, 
            SortType.ASC)
    .builder(::getArticleByIds)
    .mapper(::articleMapper)
    .build()
    
// supplier：(sortFrom: S, searchCount: Int) -> List<Pair<I, S>>
fun getArticlesByTimeAsc(timeFrom: Long, searchCount: Int): List<Pair<Long, Long>>
// builder
fun getArticleByIds(ids: Collection<Long>): Map<Long, Article>
// mapper
fun articleMapper(articles: Collection<Article>): Map<Article, ArticleView>
```

## 构建feed
* 根据是否传入查询条数，分为两个API：
```Kotlin
fun buildBy(cursorStr: String?): FeedBuilderResponse<T>

/**
 * @param cursorStr 第一次请求传入""，后续请求透传上次请求返回的[FeedBuilderResponse.nextCursor]
 * @param searchCount 查询条数 必须大于等于最大固定资源位位置，否则抛出[IllegalArgumentException]
 */
fun buildBy(cursorStr: String?, searchCount: Int): FeedBuilderResponse<T>
```

* 使用示例：
```Kotlin
val response = feedBuilder.buildBy("")
val articleViews: List<ArticleView> = response.list
val nextCursor: String = response.nextCursor
```

## 自定义参数
### 每次获取的资源条数
* 默认值：10，可通过`searchCount`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .searchCount{ 10 }
    .build()
```

### 每次获取的最大资源条数
* 如果通过`buildBy(cursorStr: String?, searchCount: Int)`API传入的searchCount值，大于该值，则按searchCount等于该值查询。
* 默认值：`Int.MAX_VALUE`，可通过`maxSearchCount`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .maxSearchCount{ 100 }
    .build()
```

### 为避免读时过滤导致多次查询增加的额外查询条数
* 默认值：3，可通过`searchBufferSize`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .searchBufferSize{ 3 }
    .build()
```

### 为避免耗时过长限制一次构建最多获取资源次数
* 读时过滤为凑够条数会多次请求supplier，为避免构建过程耗时过长，限制一次构建最多获取资源次数
* 默认值：5，可通过`searchTimesLimit`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .searchTimesLimit{ 5 }
    .build()
```

### limit大数值
* 当排序项相同的资源条量大于“每次获取的资源条数”时，一次性把该排序项对应值下的资源全部取出时的limit大数值
* 默认值：100，可通过`maxSearchBatchSize`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .maxSearchBatchSize{ 100 }
    .build()
```

### topN资源
* 默认为空，可通过`topNSupplier`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .topNSupplier { listOf(1L, 2L, 3L, 4L, 5L, 6L) }
    .build()
```

### 固定位置资源
* 默认为空，可通过`fixedSupplier`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .fixedSupplier(FixedPosition.SECOND) { listOf(6L, 7L, 8L) }
    .build()
```

### 资源构建器
* 若[集成agile4j-model-builder](#集成agile4j-model-builder)，可不声明，否则必须声明。
* [builder](#资源构建器)、[mapper](#资源映射器)必须要么都声明，要么都不声明。否则会抛出`IllegalArgumentException`异常。
* 声明API为`builder`，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .builder(::getArticleByIds)
    .mapper(::articleMapper)
    .build()

// builder
fun getArticleByIds(ids: Collection<Long>): Map<Long, Article>
```

### 资源映射器
* 若[集成agile4j-model-builder](#集成agile4j-model-builder)，可不声明，否则必须声明。
* [builder](#资源构建器)、[mapper](#资源映射器)必须要么都声明，要么都不声明。否则会抛出`IllegalArgumentException`异常。
* 声明API为`mapper`，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .builder(::getArticleByIds)
    .mapper(::articleMapper)
    .build()

// mapper
fun articleMapper(articles: Collection<Article>): Map<Article, ArticleView>
```

### 索引过滤器
* 默认不过滤，可通过`indexFilter`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .indexFilter { it > 0 }
    .build()
```

### 批量索引过滤器
* 默认不过滤，可通过`batchIndexFilter`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .batchIndexFilter { ids -> ids.associateWith { it > 0 } }
    .build()
```

### 伴生资源过滤器
* 默认不过滤，可通过`filter`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .filter { it.id > 0 }
    .build()
```

### 映射目标过滤器
* 默认不过滤，可通过`targetFilter`API自定义，例如：
```Kotlin
val feedBuilder = FeedBuilderFactory
    .ascLongBuilderEx(Article::class, ArticleView::class, ::getArticlesByTimeAsc)
    .targetFilter { view -> view.article.id > 0 }
    .build()
```

# 高级特性
## 集成agile4j-model-builder
* agile4j-model-builder：https://github.com/agile4j/agile4j-model-builder
* 集成方式为通过agile4j-model-builder的`buildBy`、`accompanyby`API进行index、accompany、target之间的关系声明。
* 集成后可不声明[builder](#资源构建器)、[mapper](#资源映射器)，构建、映射过程会自动托管到agile4j-model-builder。若声明了[builder](#资源构建器)、[mapper](#资源映射器)，则不会进行托管。
* 集成后有性能优势。因为一次feed构建过程中的多次资源构建，会共用agile4j-model-builder的缓存。