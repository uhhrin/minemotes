package com.thatnubarin.features.emotes

import com.thatnubarin.core.feature.FeatureContext
import com.thatnubarin.core.feature.ModFeature
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent
import net.minecraft.network.chat.ChatDecorator
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation

object EmotesFeature : ModFeature {
    override val id: String = "emotes"
    private val emoteFont = ResourceLocation.fromNamespaceAndPath("minemotes", "default")

    override fun initialize(context: FeatureContext) {
        EmoteRegistry.load(context.logger)

        ServerMessageDecoratorEvent.EVENT.register(ChatDecorator { _, message ->
            val original = message.string
            val rewritten = EmoteTextRewriter.rewrite(original)

            if (rewritten == original) {
                message
            } else {
                createMixedFontComponent(rewritten, message.style)
            }
        })

        context.logger.info("Emotes enabled for chat and sign text.")
    }

    private fun createMixedFontComponent(text: String, baseStyle: Style): Component {
        if (text.isEmpty()) {
            return Component.literal("").withStyle(baseStyle)
        }

        val root: MutableComponent = Component.empty()
        val runBuilder = StringBuilder()
        var runIsEmote = false
        var hasRun = false

        var cursor = 0
        while (cursor < text.length) {
            val codePoint = text.codePointAt(cursor)
            val glyph = String(Character.toChars(codePoint))
            val isEmoteGlyph = isEmoteCodePoint(codePoint)

            if (!hasRun) {
                runIsEmote = isEmoteGlyph
                hasRun = true
            } else if (isEmoteGlyph != runIsEmote) {
                appendRun(root, runBuilder.toString(), baseStyle, runIsEmote)
                runBuilder.setLength(0)
                runIsEmote = isEmoteGlyph
            }

            runBuilder.append(glyph)
            cursor += Character.charCount(codePoint)
        }

        if (runBuilder.isNotEmpty()) {
            appendRun(root, runBuilder.toString(), baseStyle, runIsEmote)
        }

        return root
    }

    private fun appendRun(root: MutableComponent, runText: String, baseStyle: Style, isEmoteRun: Boolean) {
        if (runText.isEmpty()) {
            return
        }

        val style = if (isEmoteRun) {
            baseStyle.withFont(emoteFont)
        } else {
            baseStyle
        }

        root.append(Component.literal(runText).withStyle(style))
    }

    private fun isEmoteCodePoint(codePoint: Int): Boolean =
        codePoint in 0xE000..0xF8FF
}
