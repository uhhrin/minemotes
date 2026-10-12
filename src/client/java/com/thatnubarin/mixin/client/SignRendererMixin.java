package com.thatnubarin.mixin.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.renderer.blockentity.SignRenderer.class)
public class SignRendererMixin {
    @Inject(method = "getDarkColor", at = @At("HEAD"), cancellable = true)
    private static void Minemotes$forceEmoteSignColor(SignText signText, CallbackInfoReturnable<Integer> cir) {
        if (containsEmoteGlyph(signText.getMessages(true)) || containsEmoteGlyph(signText.getMessages(false))) {
            // White keeps emote bitmap colors intact on signs/hanging signs.
            cir.setReturnValue(0x00FFFFFF);
        }
    }

    private static boolean containsEmoteGlyph(Component[] lines) {
        for (Component line : lines) {
            String text = line.getString();
            for (int i = 0; i < text.length(); i++) {
                char value = text.charAt(i);
                if (value >= '\uE000' && value <= '\uF8FF') {
                    return true;
                }
            }
        }
        return false;
    }
}
