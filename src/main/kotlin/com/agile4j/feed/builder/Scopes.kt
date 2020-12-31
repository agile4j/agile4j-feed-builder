package com.agile4j.feed.builder

import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.utils.scope.ContextScopeKey

/**
 * @author liurenpeng
 * Created on 2020-11-01
 */
object Scopes {
    fun getModelBuilderWithInit(): ModelBuilder {
        var modelBuilder = nullableModelBuilder()
        if (modelBuilder != null) {
            return modelBuilder
        }
        modelBuilder = ModelBuilder()
        setModelBuilder(modelBuilder)
        return modelBuilder
    }
    fun setModelBuilder(modelBuilder: ModelBuilder?) = modelBuilderScopeKey.set(modelBuilder)
    private fun nullableModelBuilder() = modelBuilderScopeKey.get()
    private val modelBuilderScopeKey: ContextScopeKey<ModelBuilder?> = ContextScopeKey
        .withDefaultValue(null)
}