package com.thatnubarin.features.emotes

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.slf4j.Logger
import java.nio.charset.StandardCharsets

object EmoteRegistry {
    private val gson = Gson()
    @Volatile
    private var emoteSequences: Map<String, String> = emptyMap()
    @Volatile
    private var emoteFramesByBaseGlyph: Map<String, List<String>> = emptyMap()

    fun load(logger: Logger) {
        val resourcePath = "assets/minemotes/emotes.json"
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)

        if (stream == null) {
            emoteSequences = emptyMap()
            emoteFramesByBaseGlyph = emptyMap()
            logger.error("Could not load {}. Emotes are disabled.", resourcePath)
            return
        }

        try {
            stream.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<Map<String, List<String>>>() {}.type
                val parsed = gson.fromJson<Map<String, List<String>>>(reader, type) ?: emptyMap()
                val normalizedFramesByName = parsed.mapNotNull { (name, frames) ->
                    val normalizedFrames = frames.mapNotNull(::normalizeGlyph).distinct()
                    if (normalizedFrames.isEmpty()) return@mapNotNull null
                    name.lowercase() to normalizedFrames
                }.toMap()

                emoteSequences = normalizedFramesByName.mapValues { (_, frames) -> frames.first() }

                val byBaseGlyph = LinkedHashMap<String, List<String>>(normalizedFramesByName.size)
                normalizedFramesByName.values.forEach { frames ->
                    byBaseGlyph.putIfAbsent(frames.first(), frames)
                }
                emoteFramesByBaseGlyph = byBaseGlyph
            }

            logger.info("Loaded {} emote definitions.", emoteSequences.size)
        } catch (exception: Exception) {
            emoteSequences = emptyMap()
            emoteFramesByBaseGlyph = emptyMap()
            logger.error("Failed to parse {}. Emotes are disabled.", resourcePath, exception)
        }
    }

    fun sequenceFor(emoteName: String): String? = emoteSequences[emoteName.lowercase()]

    fun hasAnimatedFramesForGlyph(glyph: String): Boolean =
        (emoteFramesByBaseGlyph[glyph]?.size ?: 0) > 1

    fun frameForGlyph(baseGlyph: String, gameTime: Long, ticksPerFrame: Int): String? {
        if (ticksPerFrame <= 0) return baseGlyph

        val frames = emoteFramesByBaseGlyph[baseGlyph] ?: return null
        if (frames.size <= 1) return baseGlyph

        val frameIndex = ((gameTime / ticksPerFrame.toLong()) % frames.size.toLong()).toInt()
        return frames[frameIndex]
    }

    fun emoteNames(): List<String> = emoteSequences.keys.sorted()

    private fun normalizeGlyph(raw: String): String? {
        if (raw.isEmpty()) {
            return null
        }

        val codePoint = raw.codePointAt(0)
        return String(Character.toChars(codePoint))
    }
}
