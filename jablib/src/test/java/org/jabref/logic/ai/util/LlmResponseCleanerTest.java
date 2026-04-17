package org.jabref.logic.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmResponseCleanerTest {
    @Test
    void nofences_plainText_returnsStripped() {
        String input = "  Hello, world!  ";
        assertEquals("Hello, world!", LlmResponseCleaner.clean(input));
    }

    @Test
    void nofences_alreadyTrimmed_returnsUnchanged() {
        String input = "Just a sentence.";
        assertEquals("Just a sentence.", LlmResponseCleaner.clean(input));
    }

    @Test
    void nofences_multiLine_returnsStripped() {
        String input = "\n  line1\n  line2\n";
        assertEquals("line1\n  line2", LlmResponseCleaner.clean(input));
    }

    @Test
    void nofences_null_returnsEmpty() {
        assertEquals("", LlmResponseCleaner.clean(null));
    }

    @Test
    void nofences_emptyString_returnsEmpty() {
        assertEquals("", LlmResponseCleaner.clean(""));
    }

    @Test
    void nofences_whitespaceOnly_returnsEmpty() {
        assertEquals("", LlmResponseCleaner.clean("   \n\t  "));
    }

    @Test
    void singleBlock_noLabel_returnsContent() {
        String input = "```\nHello\n```";
        assertEquals("Hello", LlmResponseCleaner.clean(input));
    }

    @Test
    void singleBlock_noLabel_contentWithInternalNewlines() {
        String input = "```\nline1\nline2\nline3\n```";
        assertEquals("line1\nline2\nline3", LlmResponseCleaner.clean(input));
    }

    @Test
    void singleBlock_withLeadingText() {
        String input = "Here is the result:\n```\ncontent\n```";
        assertEquals("content", LlmResponseCleaner.clean(input));
    }

    @Test
    void singleBlock_withTrailingText() {
        String input = "```\ncontent\n```\nSome trailing note.";
        assertEquals("content", LlmResponseCleaner.clean(input));
    }

    @Test
    void singleBlock_jsonLabel_labelStripped() {
        String input = "```json\n{\"key\": \"value\"}\n```";
        assertEquals("{\"key\": \"value\"}", LlmResponseCleaner.clean(input));
    }

    @Test
    void singleBlock_markdownLabel_labelStripped() {
        String input = "```markdown\n# Title\nBody text.\n```";
        assertEquals("# Title\nBody text.", LlmResponseCleaner.clean(input));
    }

    @Test
    void singleBlock_javaLabel_labelStripped() {
        String input = "```java\npublic class Foo {}\n```";
        assertEquals("public class Foo {}", LlmResponseCleaner.clean(input));
    }

    @Test
    void singleBlock_xmlLabel_labelStripped() {
        String input = "```xml\n<root/>\n```";
        assertEquals("<root/>", LlmResponseCleaner.clean(input));
    }

    @Test
    void singleBlock_labelWithSpaces_labelStripped() {
        // Unusual but should not blow up; the whole first line after ``` is the label line.
        String input = "```  json  \n{}\n```";
        assertEquals("{}", LlmResponseCleaner.clean(input));
    }

    @Test
    void multipleBlocks_returnsLastBlock() {
        String input = "```\nfirst block\n```\nsome text\n```\nsecond block\n```";
        assertEquals("second block", LlmResponseCleaner.clean(input));
    }

    @Test
    void multipleBlocks_threeBlocks_returnsThird() {
        String input = "```\nA\n```\n```\nB\n```\n```\nC\n```";
        assertEquals("C", LlmResponseCleaner.clean(input));
    }

    @Test
    void multipleBlocks_lastBlockHasLabel_labelStripped() {
        String input = "```\nfirst\n```\n```json\n{\"x\":1}\n```";
        assertEquals("{\"x\":1}", LlmResponseCleaner.clean(input));
    }

    @Test
    void multipleBlocks_firstHasLabelLastDoesNot() {
        String input = "```json\n{}\n```\n```\nplain content\n```";
        assertEquals("plain content", LlmResponseCleaner.clean(input));
    }

    @Test
    void unclosedFence_noLabel_returnsEverythingAfterFenceLine() {
        String input = "```\nsome content without closing";
        assertEquals("some content without closing", LlmResponseCleaner.clean(input));
    }

    @Test
    void unclosedFence_withLabel_returnsEverythingAfterFenceLine() {
        String input = "```json\n{\"partial\": true";
        assertEquals("{\"partial\": true", LlmResponseCleaner.clean(input));
    }

    @Test
    void unclosedFence_afterClosedBlock_treatedAsLastOpener() {
        // Two fences: first closes a block, second opens but never closes.
        // The last opener is the third ```, so content is what follows it.
        String input = "```\nfirst\n```\n```\nunclosed content";
        assertEquals("unclosed content", LlmResponseCleaner.clean(input));
    }

    @Test
    void emptyBlock_returnsEmpty() {
        String input = "```\n```";
        assertEquals("", LlmResponseCleaner.clean(input));
    }

    @Test
    void emptyBlockWithLabel_returnsEmpty() {
        String input = "```json\n```";
        assertEquals("", LlmResponseCleaner.clean(input));
    }

    @Test
    void contentInsideBlockIsNotTrimmedInternally_onlyOuterStrip() {
        // Internal indentation must be preserved; only outer strip applied.
        String input = "```\n  indented line\n    more indent\n```";
        assertEquals("  indented line\n    more indent", LlmResponseCleaner.clean(input));
    }

    @Test
    void fenceOnSameLineAsContent_noNewlineAfterLabel() {
        // Opening fence has no newline at all → content is empty / whatever follows.
        String input = "```json```";
        // After finding opener at 0, label line has no '\n', contentStart == afterFenceMarker (7→ "json```")
        // closing ``` is found at index 7, so content is "" which strips to "".
        assertEquals("", LlmResponseCleaner.clean(input));
    }

    @Test
    void surroundingWhitespace_isStripped() {
        String input = "  ```\n  content  \n```  ";
        assertEquals("content", LlmResponseCleaner.clean(input));
    }

    @Test
    void realWorldJsonResponse() {
        String input = """
                Sure! Here is the JSON you requested:

                ```json
                {
                  "name": "Alice",
                  "age": 30
                }
                ```

                Let me know if you need anything else.
                """;
        String expected = """
                {
                  "name": "Alice",
                  "age": 30
                }""";
        assertEquals(expected, LlmResponseCleaner.clean(input));
    }

    @Test
    void realWorldMarkdownResponse() {
        String input = "Here's a summary:\n\n```markdown\n# Summary\n\n- Point A\n- Point B\n```";
        assertEquals("# Summary\n\n- Point A\n- Point B", LlmResponseCleaner.clean(input));
    }
}
