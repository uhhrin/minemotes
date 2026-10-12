package com.thatnubarin.core.feature

class FeatureRegistry(
    private val context: FeatureContext
) {
    private val features = mutableListOf<ModFeature>()

    fun register(feature: ModFeature): FeatureRegistry {
        features += feature
        return this
    }

    fun initializeAll() {
        for (feature in features) {
            context.logger.info("Initializing feature '{}'.", feature.id)
            feature.initialize(context)
        }
    }
}
