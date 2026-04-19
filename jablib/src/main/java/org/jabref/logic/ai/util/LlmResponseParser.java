package org.jabref.logic.ai.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses structured lists from LLM response strings.
 *
 * <p>This utility extracts numbered or bulleted list items from LLM responses,
 * handling common formatting patterns like:
 * <ul>
 *   <li>Numbered lists: {@code 1. Item one}, {@code 2. Item two}</li>
 *   <li>Bulleted lists: {@code - Item}, {@code * Item}, {@code • Item}</li>
 * </ul>
 *
 * <p>The parser tries numbered format first (multiline regex matching).
 * If no numbered items are found, it falls back to line-by-line parsing
 * that handles both numbered and bulleted formats.
 *
 * <p>Extracted items have quotes stripped and blank entries are filtered out.
 */
public final class LlmResponseParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(LlmResponseParser.class);
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\s*\\d+\\.\\s*(.+)$", Pattern.MULTILINE);
    private static final String QUOTE_REMOVAL_PATTERN = "^[\"']|[\"']$";

    private LlmResponseParser() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /**
     * Extracts a numbered or bulleted list from an LLM response.
     *
     * <p>Parsing strategy:
     * <ol>
     *   <li>First attempts to extract numbered items using regex (e.g., "1. Question")</li>
     *   <li>If no numbered items found, falls back to line-by-line parsing that handles
     *       both numbered and bulleted formats (-, *, •)</li>
     *   <li>Removes surrounding quotes from each item</li>
     *   <li>Filters out blank or null items</li>
     * </ol>
     *
     * @param response the raw LLM response; may be {@code null}
     * @return a list of extracted non-blank items; empty list if no valid items found or input is {@code null}
     */
    public static List<String> extractNumberedList(String response) {
        if (response == null) {
            return new ArrayList<>();
        }

        List<String> items = new ArrayList<>();

        // Try numbered format first (multiline regex)
        Matcher matcher = NUMBERED_PATTERN.matcher(response);

        while (matcher.find()) {
            String item = matcher.group(1).trim();
            item = item.replaceAll(QUOTE_REMOVAL_PATTERN, "");

            if (isValidItem(item)) {
                items.add(item);
            }
        }

        // If numbered format didn't match, try line-by-line parsing
        if (items.isEmpty()) {
            LOGGER.debug("Numbered format parsing failed, trying line-by-line parsing");
            String[] lines = response.split("\n");

            for (String line : lines) {
                line = line.trim()
                           .replaceAll("^[-*•]\\s*", "")  // Remove bullet points
                           .replaceAll("^\\d+\\.\\s*", "")  // Remove numbered prefix
                           .replaceAll(QUOTE_REMOVAL_PATTERN, "");  // Remove quotes

                if (isValidItem(line)) {
                    items.add(line);
                }
            }
        }

        return items;
    }

    private static boolean isValidItem(String item) {
        return item != null && !item.isBlank();
    }
}

