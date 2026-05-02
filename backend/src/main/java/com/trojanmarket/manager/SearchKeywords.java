package com.trojanmarket.manager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure-logic helpers for the search-relevance algorithm in CLAUDE.md.
 *
 * Extracted from {@link SearchManager} so the parsing pipeline and scoring rules
 * can be unit-tested without standing up a database.
 *
 * Scoring rules per keyword (summed across keywords):
 *   +3 — keyword is a whole word in the title
 *   +2 — keyword is a substring of the title (and not a whole word)
 *   +1 — keyword is a substring of the description (independent of title)
 */
public final class SearchKeywords {

    private SearchKeywords() {
    }

    /**
     * Trim, lowercase, strip non-alphanumeric/space characters, and split on
     * whitespace. Empty strings are filtered out. Returns an empty list for null
     * or blank input.
     */
    public static List<String> clean(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String cleaned = query.trim().toLowerCase().replaceAll("[^a-z0-9 ]", " ");
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    /**
     * Returns the relevance score for a posting given the cleaned keyword list.
     * Returns 0 when keywords is empty.
     */
    public static int score(String title, String description, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return 0;
        }
        String titleLower = title == null ? "" : title.toLowerCase();
        String descLower = description == null ? "" : description.toLowerCase();
        Set<String> titleWords = new HashSet<>(Arrays.asList(titleLower.split("\\W+")));

        int total = 0;
        for (String kw : keywords) {
            if (titleWords.contains(kw)) {
                total += 3;
            } else if (titleLower.contains(kw)) {
                total += 2;
            }
            if (descLower.contains(kw)) {
                total += 1;
            }
        }
        return total;
    }
}
