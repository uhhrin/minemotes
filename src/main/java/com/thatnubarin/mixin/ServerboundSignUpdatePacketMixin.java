package com.thatnubarin.mixin;

import com.thatnubarin.features.emotes.EmoteTextRewriter;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundSignUpdatePacket.class)
public class ServerboundSignUpdatePacketMixin {
    @Inject(method = "getLines", at = @At("RETURN"), cancellable = true)
    private void Minemotes$rewriteSignText(CallbackInfoReturnable<String[]> cir) {
        String[] originalLines = cir.getReturnValue();
        String[] rewrittenLines = new String[originalLines.length];

        boolean changed = false;
        for (int i = 0; i < originalLines.length; i++) {
            String rewritten = EmoteTextRewriter.rewrite(originalLines[i]);
            rewrittenLines[i] = rewritten;
            changed |= !rewritten.equals(originalLines[i]);
        }

        if (changed) {
            cir.setReturnValue(rewrittenLines);
        }
    }
}
