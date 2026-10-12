package com.thatnubarin.mixin.client;

import com.thatnubarin.client.emotes.EmoteAutocomplete;
import com.thatnubarin.features.emotes.EmoteRegistry;
import com.thatnubarin.features.emotes.EmoteTextRewriter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {
    @Shadow @Final private SignBlockEntity sign;
    @Shadow private String[] messages;
    @Shadow private int line;
    @Shadow private TextFieldHelper signField;

    @Unique private static final ResourceLocation Minemotes$EMOTE_FONT = ResourceLocation.fromNamespaceAndPath("minemotes", "default");
    @Unique private static final int Minemotes$MAX_MATCHES = 6;
    @Unique private static final int Minemotes$BG_COLOR = 0xE014171C;
    @Unique private static final int Minemotes$ROW_SELECTED = 0xF03A414D;
    @Unique private static final int Minemotes$TEXT_NORMAL = 0xFFE3E5EA;
    @Unique private static final int Minemotes$TEXT_SELECTED = 0xFFFFFFFF;
    @Unique private static final int Minemotes$PREVIEW_COLOR = 0xFFFFFFFF;
    @Unique private static final int Minemotes$PREVIEW_WIDTH = 12;
    @Unique private static final String Minemotes$NEGATIVE_SPACE = "\uF801";

    @Unique private final List<String> Minemotes$matches = new ArrayList<>();
    @Unique private boolean Minemotes$pickerVisible = false;
    @Unique private int Minemotes$selectedIndex = 0;
    @Unique private int Minemotes$tokenStartIndex = -1;
    @Unique private int Minemotes$activeLine = -1;
    @Unique private String Minemotes$query = "";

    protected AbstractSignEditScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void Minemotes$onSignKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        this.Minemotes$refreshPicker();
        if (this.Minemotes$handlePickerKey(keyCode)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_45658", at = @At("HEAD"), cancellable = true)
    private void Minemotes$validateWithRewrittenEmotes(String candidate, CallbackInfoReturnable<Boolean> cir) {
        if (candidate == null || this.font == null || this.sign == null) {
            return;
        }

        String rewritten = EmoteTextRewriter.rewrite(candidate);
        cir.setReturnValue(this.font.width(rewritten) <= this.sign.getMaxTextLineWidth());
    }

    @Inject(method = "renderSignText", at = @At("TAIL"))
    private void Minemotes$renderSignPicker(GuiGraphics guiGraphics, CallbackInfo ci) {
        this.Minemotes$refreshPicker();
        if (!this.Minemotes$pickerVisible || this.Minemotes$matches.isEmpty()) {
            return;
        }

        Font font = this.font;
        if (font == null) {
            return;
        }

        String lineText = this.Minemotes$currentLineText();
        int cursor = this.Minemotes$clamp(this.signField.getCursorPos(), 0, lineText.length());
        int safeTokenStart = this.Minemotes$clamp(this.Minemotes$tokenStartIndex, 0, cursor);

        int lineHeight = Math.max(1, this.sign.getTextLineHeight());
        int halfTextBlockHeight = (4 * lineHeight) / 2;
        int textBaselineY = this.line * lineHeight - halfTextBlockHeight;

        String displayText = font.isBidirectional() ? font.bidirectionalShaping(lineText) : lineText;
        int safeDisplayStart = this.Minemotes$clamp(safeTokenStart, 0, displayText.length());
        int fullLineWidth = font.width(displayText);
        int leftX = -(fullLineWidth / 2);
        int anchorX = leftX + font.width(displayText.substring(0, safeDisplayStart));

        int rowHeight = font.lineHeight + 3;
        int boxHeight = this.Minemotes$matches.size() * rowHeight + 6;
        int boxWidth = this.Minemotes$pickerWidth(font);

        int x = anchorX - 5;
        int y = textBaselineY - boxHeight - 2;
        if (y < -halfTextBlockHeight - boxHeight) {
            y = textBaselineY + lineHeight + 2;
        }

        int maxTextWidth = Math.max(40, this.sign.getMaxTextLineWidth());
        int minX = -maxTextWidth / 2 - 8;
        int maxX = maxTextWidth / 2 + 8 - boxWidth;
        if (maxX >= minX) {
            x = this.Minemotes$clamp(x, minX, maxX);
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 80.0F);

        guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, Minemotes$BG_COLOR);

        for (int i = 0; i < this.Minemotes$matches.size(); i++) {
            boolean selected = i == this.Minemotes$selectedIndex;
            int rowY = y + 3 + i * rowHeight;

            if (selected) {
                guiGraphics.fill(x + 1, rowY, x + boxWidth - 1, rowY + rowHeight - 1, Minemotes$ROW_SELECTED);
            }

            String emoteName = this.Minemotes$matches.get(i);
            String glyph = EmoteRegistry.INSTANCE.sequenceFor(emoteName);
            Component preview = Component.literal(glyph == null ? "?" : glyph).withStyle(Style.EMPTY.withFont(Minemotes$EMOTE_FONT));
            guiGraphics.drawString(font, preview, x + 3, rowY + 1, Minemotes$PREVIEW_COLOR, false);

            String label = ":" + emoteName + ":";
            guiGraphics.drawString(
                font,
                label,
                x + 3 + Minemotes$PREVIEW_WIDTH,
                rowY + 1,
                selected ? Minemotes$TEXT_SELECTED : Minemotes$TEXT_NORMAL,
                false
            );
        }

        guiGraphics.pose().popPose();
    }

    @Unique
    private void Minemotes$refreshPicker() {
        if (this.signField == null || this.messages == null || this.line < 0 || this.line >= this.messages.length) {
            this.Minemotes$hidePicker();
            return;
        }

        String lineText = this.Minemotes$currentLineText();
        int cursor = this.Minemotes$clamp(this.signField.getCursorPos(), 0, lineText.length());
        EmoteAutocomplete.Token token = EmoteAutocomplete.findToken(lineText, cursor);

        if (token == null) {
            this.Minemotes$hidePicker();
            return;
        }

        List<String> found = EmoteAutocomplete.search(token.query(), EmoteRegistry.INSTANCE.emoteNames(), Minemotes$MAX_MATCHES);
        if (found.isEmpty()) {
            this.Minemotes$hidePicker();
            return;
        }

        boolean queryChanged = token.startIndex() != this.Minemotes$tokenStartIndex
            || this.line != this.Minemotes$activeLine
            || !token.query().equals(this.Minemotes$query);

        this.Minemotes$activeLine = this.line;
        this.Minemotes$tokenStartIndex = token.startIndex();
        this.Minemotes$query = token.query();
        this.Minemotes$matches.clear();
        this.Minemotes$matches.addAll(found);

        if (queryChanged) {
            this.Minemotes$selectedIndex = 0;
        } else if (this.Minemotes$selectedIndex >= this.Minemotes$matches.size()) {
            this.Minemotes$selectedIndex = this.Minemotes$matches.size() - 1;
        }

        this.Minemotes$pickerVisible = true;
    }

    @Unique
    private boolean Minemotes$handlePickerKey(int keyCode) {
        if (!this.Minemotes$pickerVisible || this.Minemotes$matches.isEmpty()) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_UP) {
            this.Minemotes$selectedIndex = (this.Minemotes$selectedIndex - 1 + this.Minemotes$matches.size()) % this.Minemotes$matches.size();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            this.Minemotes$selectedIndex = (this.Minemotes$selectedIndex + 1) % this.Minemotes$matches.size();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            this.Minemotes$applySelection();
            return true;
        }

        return false;
    }

    @Unique
    private void Minemotes$applySelection() {
        if (this.signField == null || this.Minemotes$matches.isEmpty() || this.line < 0 || this.line >= this.messages.length) {
            return;
        }

        if (this.Minemotes$selectedIndex < 0 || this.Minemotes$selectedIndex >= this.Minemotes$matches.size()) {
            this.Minemotes$selectedIndex = 0;
        }

        String lineText = this.Minemotes$currentLineText();
        int cursor = this.Minemotes$clamp(this.signField.getCursorPos(), 0, lineText.length());
        int safeTokenStart = this.Minemotes$clamp(this.Minemotes$tokenStartIndex, 0, cursor);

        this.signField.setCursorPos(safeTokenStart);
        this.signField.setSelectionPos(cursor);

        String emoteName = this.Minemotes$matches.get(this.Minemotes$selectedIndex);
        String glyph = EmoteRegistry.INSTANCE.sequenceFor(emoteName);
        boolean needsSpace = cursor >= lineText.length() || !Character.isWhitespace(lineText.charAt(cursor));
        String insertion = glyph == null ? ":" + emoteName + ":" : glyph + Minemotes$NEGATIVE_SPACE;
        if (needsSpace) {
            insertion += " ";
        }
        this.signField.insertText(insertion);
        this.Minemotes$refreshPicker();
    }

    @Unique
    private int Minemotes$pickerWidth(Font font) {
        int contentWidth = 0;
        for (String match : this.Minemotes$matches) {
            contentWidth = Math.max(contentWidth, font.width(":" + match + ":"));
        }
        return Math.max(120, contentWidth + Minemotes$PREVIEW_WIDTH + 12);
    }

    @Unique
    private String Minemotes$currentLineText() {
        String value = this.messages[this.line];
        return value == null ? "" : value;
    }

    @Unique
    private void Minemotes$hidePicker() {
        this.Minemotes$pickerVisible = false;
        this.Minemotes$matches.clear();
        this.Minemotes$selectedIndex = 0;
        this.Minemotes$tokenStartIndex = -1;
        this.Minemotes$activeLine = -1;
        this.Minemotes$query = "";
    }

    @Unique
    private int Minemotes$clamp(int value, int min, int max) {
        if (value < min) return min;
        return Math.min(value, max);
    }
}
