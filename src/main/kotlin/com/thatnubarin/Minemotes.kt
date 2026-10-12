package com.thatnubarin

import com.thatnubarin.core.feature.FeatureContext
import com.thatnubarin.core.feature.FeatureRegistry
import com.thatnubarin.features.emotes.EmotesFeature
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object Minemotes : ModInitializer {
    private val logger = LoggerFactory.getLogger("minemotes")

	override fun onInitialize() {
		val featureRegistry = FeatureRegistry(FeatureContext(logger))
		featureRegistry
			.register(EmotesFeature)
			.initializeAll()
	}
}
