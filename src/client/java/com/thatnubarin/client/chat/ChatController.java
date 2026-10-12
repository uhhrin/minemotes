package com.thatnubarin.client.chat;

import com.thatnubarin.client.emotes.EmoteAutocomplete;
import com.thatnubarin.features.emotes.EmoteRegistry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Central point for chat UI behaviors so new chat features can be added in one place.
 */
public final class ChatController {
    private final EmotePicker emotePicker = new EmotePicker();

    public boolean onKeyPressed(int keyCode, EditBox input, boolean commandSuggestionsVisible) {
        this.emotePicker.refresh(input, commandSuggestionsVisible);
        return this.emotePicker.handleKey(keyCode, input);
    }

    public void render(
        GuiGraphics guiGraphics,
        Font font,
        EditBox input,
        int screenWidth,
        int screenHeight,
        boolean commandSuggestionsVisible
    ) {
        this.emotePicker.refresh(input, commandSuggestionsVisible);
        this.emotePicker.render(guiGraphics, font, input, screenWidth, screenHeight);
    }

    private static final class EmotePicker {
        private static final ResourceLocation EMOTE_FONT = ResourceLocation.fromNamespaceAndPath("minemotes", "default");
        private static final int MAX_MATCHES = 6;
        private static final int BACKGROUND = 0xF0181B20;
        private static final int ROW_SELECTED = 0xF0404652;
        private static final int TEXT_NORMAL = 0xFFE3E5EA;
        private static final int TEXT_SELECTED = 0xFFFFFFFF;
        private static final int PREVIEW_COLOR = 0xFFFFFFFF;
        private static final int PREVIEW_AREA_WIDTH = 14;

        private final List<String> matches = new ArrayList<>();
        private boolean visible = false;
        private int selectedIndex = 0;
        private int tokenStartIndex = -1;
        private String query = "";

        private void refresh(EditBox input, boolean commandSuggestionsVisible) {
            if (input == null) {
                this.hide();
                return;
            }

            String message = input.getValue();
            if (message.startsWith("/")) {
                this.hide();
                return;
            }

            EmoteAutocomplete.Token token = EmoteAutocomplete.findToken(message, input.getCursorPosition());
            if (token == null) {
                this.hide();
                return;
            }

            List<String> found = EmoteAutocomplete.search(token.query(), EmoteRegistry.INSTANCE.emoteNames(), MAX_MATCHES);
            if (found.isEmpty()) {
                this.hide();
                return;
            }

            boolean queryChanged = token.startIndex() != this.tokenStartIndex || !token.query().equals(this.query);
            this.tokenStartIndex = token.startIndex();
            this.query = token.query();

            this.matches.clear();
            this.matches.addAll(found);

            if (queryChanged) {
                this.selectedIndex = 0;
            } else if (this.selectedIndex >= this.matches.size()) {
                this.selectedIndex = this.matches.size() - 1;
            }

            this.visible = !commandSuggestionsVisible;
        }

        private boolean handleKey(int keyCode, EditBox input) {
            if (!this.visible || this.matches.isEmpty()) {
                return false;
            }

            if (keyCode == GLFW.GLFW_KEY_UP) {
                this.selectedIndex = (this.selectedIndex - 1 + this.matches.size()) % this.matches.size();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                this.selectedIndex = (this.selectedIndex + 1) % this.matches.size();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_TAB) {
                this.applySelected(input);
                return true;
            }

            return false;
        }

        private void render(GuiGraphics guiGraphics, Font font, EditBox input, int screenWidth, int screenHeight) {
            if (!this.visible || this.matches.isEmpty() || input == null || input.getWidth() <= 0) {
                return;
            }

            int rowHeight = font.lineHeight + 5;
            int rowCount = this.matches.size();
            int boxHeight = rowCount * rowHeight + 8;
            int y = Math.max(4, input.getY() - boxHeight - 4);
            y = Math.min(y, Math.max(4, screenHeight - boxHeight - 4));

            int contentWidth = 0;
            for (String name : this.matches) {
                String label = ":" + name + ":";
                contentWidth = Math.max(contentWidth, font.width(label));
            }

            int boxWidth = Math.max(168, contentWidth + PREVIEW_AREA_WIDTH + 18);
            boxWidth = Math.min(boxWidth, screenWidth - 4);

            int anchorX = this.resolveAnchorX(font, input);
            int x = anchorX - 6;
            if (x + boxWidth > screenWidth - 2) {
                x = screenWidth - boxWidth - 2;
            }
            if (x < 2) {
                x = 2;
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0D, 0.0D, 300.0D);

            guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, BACKGROUND);

            for (int i = 0; i < this.matches.size(); i++) {
                boolean selected = i == this.selectedIndex;
                int rowY = y + 4 + i * rowHeight;

                if (selected) {
                    guiGraphics.fill(x + 2, rowY, x + boxWidth - 2, rowY + rowHeight - 1, ROW_SELECTED);
                }

                String emoteName = this.matches.get(i);
                String emoteGlyph = EmoteRegistry.INSTANCE.sequenceFor(emoteName);
                Component preview = Component.literal(emoteGlyph == null ? "?" : emoteGlyph)
                    .withStyle(Style.EMPTY.withFont(EMOTE_FONT));
                guiGraphics.drawString(font, preview, x + 5, rowY + 1, PREVIEW_COLOR, false);

                String label = ":" + emoteName + ":";
                guiGraphics.drawString(
                    font,
                    label,
                    x + 5 + PREVIEW_AREA_WIDTH,
                    rowY + 1,
                    selected ? TEXT_SELECTED : TEXT_NORMAL,
                    false
                );
            }

            guiGraphics.pose().popPose();
        }

        private int resolveAnchorX(Font font, EditBox input) {
            String message = input.getValue();
            int messageLength = message.length();
            int cursor = Math.max(0, Math.min(input.getCursorPosition(), messageLength));
            int safeTokenStart = Math.max(0, Math.min(this.tokenStartIndex, cursor));

            int cursorX;
            try {
                cursorX = input.getScreenX(cursor);
            } catch (RuntimeException exception) {
                return input.getX() + 4;
            }

            if (safeTokenStart >= cursor) {
                return cursorX;
            }

            String tokenPrefix = message.substring(safeTokenStart, cursor);
            return cursorX - font.width(tokenPrefix);
        }

        private void applySelected(EditBox input) {
            if (!this.visible || this.matches.isEmpty() || input == null) {
                return;
            }

            if (this.selectedIndex < 0 || this.selectedIndex >= this.matches.size()) {
                this.selectedIndex = 0;
            }

            String message = input.getValue();
            int cursor = input.getCursorPosition();
            if (this.tokenStartIndex < 0 || this.tokenStartIndex > cursor || cursor > message.length()) {
                return;
            }

            String chosen = this.matches.get(this.selectedIndex);
            boolean needsSpace = cursor >= message.length() || !Character.isWhitespace(message.charAt(cursor));
            String replacement = ":" + chosen + ":" + (needsSpace ? " " : "");
            String updated = message.substring(0, this.tokenStartIndex) + replacement + message.substring(cursor);

            input.setValue(updated);
            input.setCursorPosition(this.tokenStartIndex + replacement.length());
            this.refresh(input, false);
        }

        private void hide() {
            this.visible = false;
            this.matches.clear();
            this.selectedIndex = 0;
            this.tokenStartIndex = -1;
            this.query = "";
        }
    }
}
