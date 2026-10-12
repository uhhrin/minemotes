package com.thatnubarin.mixin.client;

import com.thatnubarin.client.emotes.EmoteAnimator;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(net.minecraft.client.gui.Font.class)
public class FontMixin {
    @ModifyVariable(
        method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
        at = @At("HEAD"),
        argsOnly = true
    )
    private String Minemotes$animateStringGlyphs(String text) {
        return EmoteAnimator.animate(text);
    }

    @ModifyVariable(
        method = "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
        at = @At("HEAD"),
        argsOnly = true
    )
    private FormattedCharSequence Minemotes$animateFormattedGlyphs(FormattedCharSequence sequence) {
        return EmoteAnimator.animate(sequence);
    }
}
