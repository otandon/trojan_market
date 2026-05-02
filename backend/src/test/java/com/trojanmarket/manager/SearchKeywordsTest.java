package com.trojanmarket.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the test plan's "Search System" white-box and unit tests:
 * keyword parsing pipeline (trim/lowercase/strip-special/split) and the
 * relevance scoring rules (+3 whole-word title, +2 substring title, +1 desc).
 */
class SearchKeywordsTest {

    @Nested
    @DisplayName("clean(): keyword parsing pipeline")
    class Clean {

        @Test
        void trimsLeadingAndTrailingWhitespace() {
            assertThat(SearchKeywords.clean(" hello ")).containsExactly("hello");
        }

        @Test
        void lowercasesEverything() {
            assertThat(SearchKeywords.clean("HELLO World")).containsExactly("hello", "world");
        }

        @Test
        void stripsSpecialCharactersInline() {
            // The plan's expectation is "he!!o" → "he" + "o" with empties filtered.
            // (Non-alphanumeric/space chars are replaced with spaces, then split on whitespace.)
            assertThat(SearchKeywords.clean("he!!o @world#"))
                    .containsExactlyInAnyOrder("he", "o", "world");
        }

        @Test
        void splitsOnWhitespaceAndDropsEmpties() {
            assertThat(SearchKeywords.clean("one two   three")).containsExactly("one", "two", "three");
        }

        @Test
        void allSpecialCharsYieldsEmptyList() {
            assertThat(SearchKeywords.clean("!@#$%")).isEmpty();
        }

        @Test
        void blankOrNullYieldsEmptyList() {
            assertThat(SearchKeywords.clean(null)).isEmpty();
            assertThat(SearchKeywords.clean("")).isEmpty();
            assertThat(SearchKeywords.clean("   ")).isEmpty();
        }

        @Test
        void doubleExclamationStrippedKeepsRoot() {
            assertThat(SearchKeywords.clean("desk!!!")).containsExactly("desk");
        }

        @Test
        void caseInsensitiveYieldsSameTokens() {
            assertThat(SearchKeywords.clean(" DESK ")).isEqualTo(SearchKeywords.clean("desk"));
        }
    }

    @Nested
    @DisplayName("score(): relevance scoring")
    class Score {

        // Test data: title "Blue Wooden Desk", description "A sturdy desk made of oak wood, painted blue."
        private static final String TITLE = "Blue Wooden Desk";
        private static final String DESC = "A sturdy desk made of oak wood, painted blue.";

        @Test
        void wholeWordTitleMatchScoresThree() {
            // "desk" is a whole word in title (+3) AND appears in description (+1) → 4.
            assertThat(SearchKeywords.score(TITLE, DESC, List.of("desk"))).isEqualTo(4);
        }

        @Test
        void substringTitleMatchScoresTwo() {
            // "wood" is a substring of "Wooden" in title (not whole word) → +2; "wood" appears in desc → +1 → 3.
            assertThat(SearchKeywords.score(TITLE, DESC, List.of("wood"))).isEqualTo(3);
        }

        @Test
        void descriptionOnlyMatchScoresOne() {
            // "oak" is only in description.
            assertThat(SearchKeywords.score(TITLE, DESC, List.of("oak"))).isEqualTo(1);
        }

        @Test
        void multipleKeywordsAccumulate() {
            // "blue" — whole word in title (+3) + appears in desc (+1) = 4
            // "desk" — whole word in title (+3) + appears in desc (+1) = 4
            // total = 8.
            assertThat(SearchKeywords.score(TITLE, DESC, List.of("blue", "desk"))).isEqualTo(8);
        }

        @Test
        void noMatchKeywordScoresZero() {
            assertThat(SearchKeywords.score(TITLE, DESC, List.of("piano"))).isZero();
        }

        @Test
        void titleWholeWordTakesPrecedenceOverSubstring() {
            // "blue" is a whole word in title (+3, not +2) AND in description (+1) → 4.
            assertThat(SearchKeywords.score(TITLE, DESC, List.of("blue"))).isEqualTo(4);
        }

        @Test
        void emptyKeywordsReturnsZero() {
            assertThat(SearchKeywords.score(TITLE, DESC, List.of())).isZero();
            assertThat(SearchKeywords.score(TITLE, DESC, null)).isZero();
        }

        @Test
        void nullTitleAndDescriptionAreSafe() {
            assertThat(SearchKeywords.score(null, null, List.of("anything"))).isZero();
        }

        @Test
        void substringTitleNotWholeWordWhenContainsLargerWord() {
            // "wood" is a substring of "Wooden" → not a whole word → +2.
            // (No description match for "wooden" but "wood" matches both.)
            assertThat(SearchKeywords.score("Wooden Desk", "no relevant words", List.of("wood")))
                    .isEqualTo(2);
        }
    }
}
