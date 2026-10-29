package org.jabref.logic.util.strings;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexPatternsTest {

    @Test
    void whitespaceMatchesOneOrMoreSpaces() {
        assertTrue(RegexPatterns.WHITESPACE.matcher("   ").matches());
        assertTrue(RegexPatterns.WHITESPACE.matcher("\t\n ").matches());
        assertEquals("a b", RegexPatterns.WHITESPACE.matcher("a   b").replaceAll(" "));
    }

    @Test
    void singleWhitespaceMatchesSingleCharacter() {
        assertTrue(RegexPatterns.SINGLE_WHITESPACE.matcher(" ").matches());
        assertTrue(RegexPatterns.SINGLE_WHITESPACE.matcher("\t").matches());
        assertFalse(RegexPatterns.SINGLE_WHITESPACE.matcher("  ").matches());
    }

    @Test
    void multipleSpacesMatchesTwoOrMoreSpaces() {
        assertFalse(RegexPatterns.MULTIPLE_SPACES.matcher(" ").matches());
        assertTrue(RegexPatterns.MULTIPLE_SPACES.matcher("  ").matches());
        assertTrue(RegexPatterns.MULTIPLE_SPACES.matcher("    ").matches());
        assertEquals("a b c", RegexPatterns.MULTIPLE_SPACES.matcher("a    b   c").replaceAll(" "));
    }

    @Test
    void multipleWhitespacesMatchesTwoOrMoreWhitespaces() {
        assertFalse(RegexPatterns.MULTIPLE_WHITESPACES.matcher(" ").matches());
        assertTrue(RegexPatterns.MULTIPLE_WHITESPACES.matcher("\t\n").matches());
        assertTrue(RegexPatterns.MULTIPLE_WHITESPACES.matcher(" \n ").matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\r\n", "\r", "\n"})
    void lineBreaksMatchesPlatformLineEndings(String lineBreak) {
        assertTrue(RegexPatterns.LINE_BREAKS.matcher(lineBreak).matches());
        assertEquals("a b", RegexPatterns.LINE_BREAKS.matcher("a" + lineBreak + "b").replaceAll(" "));
    }

    @Test
    void lineBreakMatchesUnicodeNewline() {
        assertTrue(RegexPatterns.LINE_BREAK.matcher("\n").matches());
        assertTrue(RegexPatterns.LINE_BREAK.matcher("\r\n").matches());
        assertEquals("ab", RegexPatterns.LINE_BREAK.matcher("a\nb").replaceAll(""));
    }

    @Test
    void hyphenatedLineBreakRemovesHyphenation() {
        assertTrue(RegexPatterns.HYPHENATED_LINE_BREAK.matcher("-\n").matches());
        assertTrue(RegexPatterns.HYPHENATED_LINE_BREAK.matcher("-\r\n").matches());
        assertEquals("hyphenation", RegexPatterns.HYPHENATED_LINE_BREAK.matcher("hy-\nphenation").replaceAll(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://jabref.org", "http://example.com/test", "ftp://files.example.org/doc.pdf"})
    void remoteLinkMatchesValidUri(String uri) {
        assertTrue(RegexPatterns.REMOTE_LINK.matcher(uri).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/local/path/to/file.pdf", "C:\\folder\\file.pdf", "relative/file.pdf"})
    void remoteLinkRejectsLocalPaths(String path) {
        assertFalse(RegexPatterns.REMOTE_LINK.matcher(path).matches());
    }

    @Test
    void urlFindsUrlInText() {
        assertTrue(RegexPatterns.URL.matcher("https://jabref.org").find());
        assertTrue(RegexPatterns.URL.matcher("Visit HTTP://JABREF.ORG for more info").find());
        assertTrue(RegexPatterns.URL.matcher("ftp://ftp.is.co.za/rfc/rfc1808.txt").find());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "123", "999999"})
    void digitsMatchesDigitsOnly(String input) {
        assertTrue(RegexPatterns.DIGITS.matcher(input).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "12a", " 123"})
    void digitsRejectsNonDigits(String input) {
        assertFalse(RegexPatterns.DIGITS.matcher(input).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "123", "-456", "+123"})
    void integerMatchesIntegers(String input) {
        if (input.startsWith("+")) {
            assertFalse(RegexPatterns.INTEGER.matcher(input).matches());
        } else {
            assertTrue(RegexPatterns.INTEGER.matcher(input).matches());
        }
    }

    @ParameterizedTest
    @MethodSource("numberTestCases")
    void numberMatchesExpectedValues(String input, boolean expected) {
        assertEquals(expected, RegexPatterns.NUMBER.matcher(input).matches());
    }

    private static Stream<Arguments> numberTestCases() {
        return Stream.of(
                Arguments.of("123", true),
                Arguments.of("-123", true),
                Arguments.of("123.456", true),
                Arguments.of("-0.5", true),
                Arguments.of("abc", false),
                Arguments.of("123.", false),
                Arguments.of(".456", false)
        );
    }

    @Test
    void htmlTagMatchesTags() {
        assertTrue(RegexPatterns.HTML_TAG.matcher("<b>").matches());
        assertTrue(RegexPatterns.HTML_TAG.matcher("</div>").matches());
        assertTrue(RegexPatterns.HTML_TAG.matcher("<span class=\"test\">").matches());
        assertEquals("Bold text", RegexPatterns.HTML_TAG.matcher("<b>Bold</b> text").replaceAll(""));
    }

    @Test
    void nonAlphanumericReplacesSpecialCharacters() {
        assertEquals("HelloWorld123", RegexPatterns.NON_ALPHANUMERIC.matcher("Hello, World! #123").replaceAll(""));
    }

    @Test
    void nonLowercaseAlphanumericReplacesUppercaseAndSpecialCharacters() {
        assertEquals("elloorld123", RegexPatterns.NON_LOWERCASE_ALPHANUMERIC.matcher("HelloWorld#123").replaceAll(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "123", "999999"})
    void zeroOrMoreDigitsMatchesEmptyAndDigits(String input) {
        assertTrue(RegexPatterns.ZERO_OR_MORE_DIGITS.matcher(input).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12a", "abc", " 123", "12 3"})
    void zeroOrMoreDigitsRejectsNonDigits(String input) {
        assertFalse(RegexPatterns.ZERO_OR_MORE_DIGITS.matcher(input).matches());
    }

    @Test
    void nonDigitsRemovesNonDigits() {
        assertEquals("123456", RegexPatterns.NON_DIGITS.matcher("ISBN 123-456").replaceAll(""));
    }

    @Test
    void pageRangeSeparatorsSplitsCorrectly() {
        String[] parts = RegexPatterns.PAGE_RANGE_SEPARATORS.split("123 - 145");
        assertEquals(2, parts.length);
        assertEquals("123", parts[0]);
        assertEquals("145", parts[1]);

        String[] partsEnDash = RegexPatterns.PAGE_RANGE_SEPARATORS.split("123\u2013145");
        assertEquals(2, partsEnDash.length);
        assertEquals("123", partsEnDash[0]);
        assertEquals("145", partsEnDash[1]);
    }

    @Test
    void unescapedAmpersandMatchesOnlyUnescapedAmpersand() {
        assertEquals("A \\& B \\& C", RegexPatterns.UNESCAPED_AMPERSAND.matcher("A & B \\& C").replaceAll("\\\\&"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Title", "123", "[Note]", "!Warning"})
    void notStartingWithLowercaseMatchesUppercaseOrSymbol(String input) {
        assertTrue(RegexPatterns.NOT_STARTING_WITH_LOWERCASE.asPredicate().test(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "note", "howPublished"})
    void notStartingWithLowercaseRejectsLowercase(String input) {
        assertFalse(RegexPatterns.NOT_STARTING_WITH_LOWERCASE.asPredicate().test(input));
    }
}
