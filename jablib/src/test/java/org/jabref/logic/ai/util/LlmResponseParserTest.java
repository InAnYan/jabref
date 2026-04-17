package org.jabref.logic.ai.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmResponseParserTest {

    @Test
    void nullInput_returnsEmptyList() {
        List<String> result = LlmResponseParser.extractNumberedList(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void emptyString_returnsEmptyList() {
        List<String> result = LlmResponseParser.extractNumberedList("");
        assertTrue(result.isEmpty());
    }

    @Test
    void whitespaceOnly_returnsEmptyList() {
        List<String> result = LlmResponseParser.extractNumberedList("   \n\t  ");
        assertTrue(result.isEmpty());
    }

    @Test
    void numberedList_simple_extractsAllItems() {
        String input = """
                1. First question
                2. Second question
                3. Third question
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("First question", "Second question", "Third question"), result);
    }

    @Test
    void numberedList_withLeadingWhitespace_extractsItems() {
        String input = """
                   1. Question one
                   2. Question two
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("Question one", "Question two"), result);
    }

    @Test
    void numberedList_withSurroundingText_extractsItems() {
        String input = """
                Here are some questions:

                1. First question
                2. Second question

                Hope this helps!
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("First question", "Second question"), result);
    }

    @Test
    void numberedList_withDoubleQuotes_removesQuotes() {
        String input = """
                1. "What is the main topic?"
                2. "How does it work?"
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("What is the main topic?", "How does it work?"), result);
    }

    @Test
    void numberedList_withSingleQuotes_removesQuotes() {
        String input = """
                1. 'First question'
                2. 'Second question'
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("First question", "Second question"), result);
    }

    @Test
    void numberedList_mixedQuotes_removesAllQuotes() {
        String input = """
                1. "Question with double quotes"
                2. 'Question with single quotes'
                3. Question without quotes
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of(
                "Question with double quotes",
                "Question with single quotes",
                "Question without quotes"
        ), result);
    }

    @Test
    void numberedList_withBlankLines_filtersOutBlanks() {
        String input = """
                1. Valid question
                2.
                3. Another valid question
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("Valid question", "Another valid question"), result);
    }

    @Test
    void bulletedList_hyphen_extractsItems() {
        String input = """
                - First question
                - Second question
                - Third question
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("First question", "Second question", "Third question"), result);
    }

    @Test
    void bulletedList_asterisk_extractsItems() {
        String input = """
                * What is the main idea?
                * How does it apply?
                * Why is it important?
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("What is the main idea?", "How does it apply?", "Why is it important?"), result);
    }

    @Test
    void bulletedList_bullet_extractsItems() {
        String input = """
                • First item
                • Second item
                • Third item
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("First item", "Second item", "Third item"), result);
    }

    @Test
    void bulletedList_withQuotes_removesQuotes() {
        String input = """
                - "Question one"
                - 'Question two'
                - Question three
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("Question one", "Question two", "Question three"), result);
    }

    @Test
    void mixedFormat_numberedAndText_usesNumberedFormat() {
        // When numbered format is found, it should use that exclusively
        String input = """
                Here are questions:
                1. Numbered question
                - Bulleted item (should not be extracted in numbered mode)
                2. Another numbered question
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("Numbered question", "Another numbered question"), result);
    }

    @Test
    void fallbackMode_plainLines_extractsNonBlankLines() {
        // If no numbered items, falls back to line-by-line
        String input = """
                First question
                Second question

                Third question
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("First question", "Second question", "Third question"), result);
    }

    @Test
    void fallbackMode_mixedBullets_extractsAll() {
        String input = """
                - Question with hyphen
                * Question with asterisk
                • Question with bullet
                Plain question
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of(
                "Question with hyphen",
                "Question with asterisk",
                "Question with bullet",
                "Plain question"
        ), result);
    }

    @Test
    void fallbackMode_withQuotes_removesQuotes() {
        String input = """
                "First question"
                'Second question'
                Third question
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("First question", "Second question", "Third question"), result);
    }

    @Test
    void realWorldExample_chatGptStyleNumbered() {
        String input = """
                Here are 3 follow-up questions based on the conversation:

                1. What are the key differences between the approaches?
                2. How can this be applied in practice?
                3. What are the potential limitations?
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of(
                "What are the key differences between the approaches?",
                "How can this be applied in practice?",
                "What are the potential limitations?"
        ), result);
    }

    @Test
    void realWorldExample_bulletedWithIntro() {
        String input = """
                Based on the discussion, here are some relevant questions:

                - How does this compare to traditional methods?
                - What are the performance implications?
                - Are there any security considerations?

                Feel free to explore these topics further.
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of(
                "How does this compare to traditional methods?",
                "What are the performance implications?",
                "Are there any security considerations?"
        ), result);
    }

    @Test
    void realWorldExample_quotedQuestions() {
        String input = """
                1. "What is the main hypothesis?"
                2. "How was the data collected?"
                3. "What are the key findings?"
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of(
                "What is the main hypothesis?",
                "How was the data collected?",
                "What are the key findings?"
        ), result);
    }

    @Test
    void edgeCase_onlyBulletPoints_fallsBackToLineByLine() {
        String input = """
                -
                *
                •
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertTrue(result.isEmpty());
    }

    @Test
    void edgeCase_numbersWithoutContent() {
        String input = """
                1.
                2.
                3.
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertTrue(result.isEmpty());
    }

    @Test
    void edgeCase_singleItem_numbered() {
        String input = "1. Single question";
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("Single question"), result);
    }

    @Test
    void edgeCase_singleItem_bulleted() {
        String input = "- Single question";
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("Single question"), result);
    }

    @Test
    void edgeCase_multilineItem_treatedAsMultipleLines() {
        // Each line is processed separately in fallback mode
        String input = """
                Question one
                that spans multiple lines
                Question two
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("Question one", "that spans multiple lines", "Question two"), result);
    }

    @Test
    void noLimit_extractsAllItems() {
        String input = """
                1. Question 1
                2. Question 2
                3. Question 3
                4. Question 4
                5. Question 5
                6. Question 6
                7. Question 7
                8. Question 8
                9. Question 9
                10. Question 10
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(10, result.size());
        assertEquals("Question 1", result.get(0));
        assertEquals("Question 10", result.get(9));
    }

    @Test
    void largeNumbers_parsesCorrectly() {
        String input = """
                100. Question one hundred
                101. Question one hundred one
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("Question one hundred", "Question one hundred one"), result);
    }

    @Test
    void numberedList_withExtraSpacing_extractsItems() {
        String input = """
                1.    Question with extra spaces
                2.  Another one
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(List.of("Question with extra spaces", "Another one"), result);
    }
}

