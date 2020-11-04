package com.agile4j.feed.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.agile4j.feed.builder.mock.Article;
import com.agile4j.feed.builder.mock.ArticleView;

/**
 * @author liurenpeng
 * Created on 2020-11-03
 */
public class JavaFunctions {

    private static LinkedHashMap<Long, Long> getWithTime(long sortFrom, int limit) {
        return new LinkedHashMap<>();
    }

    private static void tempFun(Collection<Long> ids) {

    }

    private static <T extends Long> Map<Long, Article> getArticlesByIds4j(Collection<T> ids) {
        //tempFun(ids);
        return Collections.emptyMap();
    }

    private static <T extends Article> Map<Article, ArticleView> getArticleViewMap(Collection<T> articles) {
        return Collections.emptyMap();
    }

    public static void main(String[] args) {
        FeedBuilder feedBuilder = FeedBuilderFactory.INSTANCE.descLongBuilder(
                Article.class, ArticleView.class, JavaFunctions::getWithTime)
                .builder(JavaFunctions::getArticlesByIds4j)
                .mapper(JavaFunctions::getArticleViewMap)
                .build();
    }
}
