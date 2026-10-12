package com.thatnubarin.client.emotes;

import com.thatnubarin.features.emotes.EmoteRegistry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.ArrayList;
import java.util.List;

public final class EmoteAnimator {
    private static final int TICKS_PER_FRAME = 2;

    private EmoteAnimator() {
    }

    public static String animate(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        long gameTime = currentGameTime();
        StringBuilder output = null;
        int cursor = 0;

        while (cursor < text.length()) {
            int codePoint = text.codePointAt(cursor);
            String glyph = new String(Character.toChars(codePoint));

            if (EmoteRegistry.INSTANCE.hasAnimatedFramesForGlyph(glyph)) {
                String frame = EmoteRegistry.INSTANCE.frameForGlyph(glyph, gameTime, TICKS_PER_FRAME);
                if (frame != null && !frame.equals(glyph)) {
                    if (output == null) {
                        output = new StringBuilder(text.length());
                        output.append(text, 0, cursor);
                    }
                    output.append(frame);
                } else if (output != null) {
                    output.append(glyph);
                }
            } else if (output != null) {
                output.append(glyph);
            }

            cursor += Character.charCount(codePoint);
        }

        return output == null ? text : output.toString();
    }

    public static FormattedCharSequence animate(FormattedCharSequence sequence) {
        if (sequence == null || !containsAnimatedGlyph(sequence)) {
            return sequence;
        }

        long gameTime = currentGameTime();
        List<FormattedCharSequence> segments = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style[] runStyle = new Style[1];

        sequence.accept((index, style, codePoint) -> {
            if (runStyle[0] == null) {
                runStyle[0] = style;
            } else if (!runStyle[0].equals(style)) {
                flushRun(segments, run, runStyle[0]);
                runStyle[0] = style;
            }

            String glyph = new String(Character.toChars(codePoint));
            String frame = EmoteRegistry.INSTANCE.frameForGlyph(glyph, gameTime, TICKS_PER_FRAME);
            run.append(frame == null ? glyph : frame);
            return true;
        });

        flushRun(segments, run, runStyle[0]);
        return FormattedCharSequence.composite(segments);
    }

    private static boolean containsAnimatedGlyph(FormattedCharSequence sequence) {
        boolean[] found = new boolean[1];
        FormattedCharSink detector = (index, style, codePoint) -> {
            String glyph = new String(Character.toChars(codePoint));
            if (EmoteRegistry.INSTANCE.hasAnimatedFramesForGlyph(glyph)) {
                found[0] = true;
                return false;
            }
            return true;
        };
        sequence.accept(detector);
        return found[0];
    }

    private static void flushRun(List<FormattedCharSequence> segments, StringBuilder run, Style style) {
        if (run.isEmpty()) {
            return;
        }
        segments.add(FormattedCharSequence.forward(run.toString(), style == null ? Style.EMPTY : style));
        run.setLength(0);
    }

    private static long currentGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.getGameTime();
        }
        return Util.getMillis() / 50L;
    }
}
