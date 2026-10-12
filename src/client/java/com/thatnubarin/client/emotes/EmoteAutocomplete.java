package com.thatnubarin.client.emotes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class EmoteAutocomplete {
    private EmoteAutocomplete() {
    }

    public static Token findToken(String message, int cursor) {
        if (message == null || cursor < 0 || cursor > message.length()) {
            return null;
        }

        int colonIndex = message.lastIndexOf(':', Math.max(0, cursor - 1));
        if (colonIndex < 0) {
            return null;
        }

        if (colonIndex + 1 > cursor) {
            return null;
        }

        if (colonIndex > 0 && !isBoundary(message.charAt(colonIndex - 1))) {
            return null;
        }

        String query = message.substring(colonIndex + 1, cursor);
        if (query.indexOf(':') >= 0 || query.indexOf(' ') >= 0 || query.indexOf('\n') >= 0 || query.indexOf('\t') >= 0) {
            return null;
        }

        if (query.length() > 40) {
            return null;
        }

        return new Token(colonIndex, query);
    }

    public static List<String> search(String query, Collection<String> names, int limit) {
        if (names == null || names.isEmpty() || limit <= 0) {
            return List.of();
        }

        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);

        if (normalizedQuery.isEmpty()) {
            return names.stream()
                .filter(value -> value != null && !value.isEmpty())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(limit)
                .toList();
        }

        List<ScoredEmote> scored = new ArrayList<>();

        for (String name : names) {
            if (name == null || name.isEmpty()) continue;
            int score = score(name, normalizedQuery);
            if (score >= 0) {
                scored.add(new ScoredEmote(name, score));
            }
        }

        scored.sort(
            Comparator.comparingInt(ScoredEmote::score).reversed()
                .thenComparing(ScoredEmote::name, String.CASE_INSENSITIVE_ORDER)
        );

        List<String> result = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < limit; i++) {
            result.add(scored.get(i).name());
        }
        return result;
    }

    private static int score(String name, String query) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.equals(query)) {
            return 20_000;
        }

        if (lower.startsWith(query)) {
            return 15_000 - (lower.length() - query.length());
        }

        int containsAt = lower.indexOf(query);
        if (containsAt >= 0) {
            return 12_000 - containsAt * 16 - (lower.length() - query.length());
        }

        int subsequenceScore = subsequenceScore(lower, query);
        if (subsequenceScore >= 0) {
            return 8_000 + subsequenceScore;
        }

        return -1;
    }

    private static int subsequenceScore(String candidate, String query) {
        int lastIndex = -1;
        int gapPenalty = 0;

        for (int i = 0; i < query.length(); i++) {
            int nextIndex = candidate.indexOf(query.charAt(i), lastIndex + 1);
            if (nextIndex < 0) {
                return -1;
            }

            if (lastIndex >= 0) {
                gapPenalty += (nextIndex - lastIndex - 1);
            }
            lastIndex = nextIndex;
        }

        return 1_000 - gapPenalty * 8 - (candidate.length() - query.length()) * 3;
    }

    private static boolean isBoundary(char value) {
        return Character.isWhitespace(value)
            || value == '('
            || value == '['
            || value == '{'
            || value == '"'
            || value == '\''
            || value == ':';
    }

    private record ScoredEmote(String name, int score) {
    }

    public record Token(int startIndex, String query) {
    }
}
