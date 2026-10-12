package com.thatnubarin.features.emotes

object EmoteTextRewriter {
    // Match :token: only when not embedded inside identifier characters.
    private val tokenPattern = Regex("(?<![A-Za-z0-9_]):([A-Za-z0-9_]+):(?![A-Za-z0-9_])")
    private const val NEGATIVE_SPACE = "\uF801"

    @JvmStatic
    fun rewrite(text: String): String {
        if (text.isEmpty()) return text

        return tokenPattern.replace(text) { matchResult ->
            val tokenName = matchResult.groupValues[1]
            val glyph = EmoteRegistry.sequenceFor(tokenName) ?: return@replace matchResult.value
            glyph + NEGATIVE_SPACE
        }
    }
}
