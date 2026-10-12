package com.thatnubarin.core.feature

interface ModFeature {
    val id: String

    fun initialize(context: FeatureContext)
}
