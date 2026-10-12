package com.thatnubarin

import com.thatnubarin.features.emotes.EmoteRegistry
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object MinemotesClient : ClientModInitializer {
	private val logger = LoggerFactory.getLogger("minemotes-client")

	override fun onInitializeClient() {
		EmoteRegistry.load(logger)
	}
}
