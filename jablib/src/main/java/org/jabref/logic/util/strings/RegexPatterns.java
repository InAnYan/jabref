package org.jabref.logic.util.strings;

import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;

/// Commonly used regular expression patterns across JabRef.
///
/// Feature-specific regular expressions should remain in their respective feature classes.
@NullMarked
public final class RegexPatterns {

    /// Matches one or more whitespace characters (`\s+`).
    public static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /// Matches a single whitespace character (`\s`).
    public static final Pattern SINGLE_WHITESPACE = Pattern.compile("\\s");

    /// Matches two or more consecutive space characters (` {2,}`).
    public static final Pattern MULTIPLE_SPACES = Pattern.compile(" {2,}");

    /// Matches two or more consecutive whitespace characters (`\s{2,}`).
    public static final Pattern MULTIPLE_WHITESPACES = Pattern.compile("\\s{2,}");

    /// Matches standard line breaks across platforms (`\r\n`, `\r`, or `\n`).
    public static final Pattern LINE_BREAKS = Pattern.compile("\\r\\n|\\r|\\n");

    /// Matches any Unicode newline sequence (`\R`).
    public static final Pattern LINE_BREAK = Pattern.compile("\\R");

    /// Matches a hyphen followed by a line break (`-\R`), commonly found in hyphenated words across lines.
    public static final Pattern HYPHENATED_LINE_BREAK = Pattern.compile("-\\R");

    /// Matches a remote link URI starting with a lowercase scheme (`[a-z]+://.*`).
    public static final Pattern REMOTE_LINK = Pattern.compile("[a-z]+://.*");

    /// Matches a URL starting with http, https, or ftp followed by non-whitespace characters.
    public static final Pattern URL = Pattern.compile("(?i)\\b((?:https?|ftp)://[^\\s]+)");

    /// Matches one or more digit characters (`\d+`).
    public static final Pattern DIGITS = Pattern.compile("\\d+");

    /// Matches an optional minus sign followed by one or more digits (`-?\d+`).
    public static final Pattern INTEGER = Pattern.compile("-?\\d+");

    /// Matches an optional minus sign followed by integer or decimal digits (`-?\d+(\.\d+)?`).
    public static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    /// Matches any HTML or XML tag (`<[^>]*>`).
    public static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    /// Matches any character that is not an ASCII alphanumeric character (`[^a-zA-Z0-9]`).
    public static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]");

    /// Matches any character that is not a lowercase ASCII letter or digit (`[^a-z0-9]`).
    public static final Pattern NON_LOWERCASE_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    /// Matches zero or more digit characters (`\d*`).
    public static final Pattern ZERO_OR_MORE_DIGITS = Pattern.compile("\\d*");

    /// Matches one or more non-digit characters (`\D+`).
    public static final Pattern NON_DIGITS = Pattern.compile("\\D+");

    /// Matches page range separators such as hyphens, en-dashes, em-dashes, and whitespace (`[ \-\u2013\u2014]+`).
    public static final Pattern PAGE_RANGE_SEPARATORS = Pattern.compile("[ \\-\\u2013\\u2014]+");

    /// Matches an ampersand that is not preceded by a backslash (`(?<!\\)&`).
    public static final Pattern UNESCAPED_AMPERSAND = Pattern.compile("(?<!\\\\)&");

    /// Matches a string whose first character is not a lowercase ASCII letter (`^[^a-z]`).
    public static final Pattern NOT_STARTING_WITH_LOWERCASE = Pattern.compile("^[^a-z]");

    /// Matches a string whose first character is an uppercase ASCII letter (`^[A-Z]`).
    public static final Pattern STARTS_WITH_UPPERCASE = Pattern.compile("^[A-Z]");

    /// Matches exactly four consecutive digits (`\d{4}`), commonly used for years.
    public static final Pattern FOUR_DIGITS = Pattern.compile("\\d{4}");

    /// Matches a newline surrounded by optional whitespace (`\s*\n\s*`).
    public static final Pattern NEWLINE_WITH_SPACES = Pattern.compile("\\s*\\n\\s*");

    /// Matches two digits separated by a hyphen with optional whitespace (`([0-9]) *- *([0-9])`), used in page range normalization.
    public static final Pattern PAGE_NUMBERS_DASH = Pattern.compile("([0-9]) *- *([0-9])");

    /// Matches Roman numerals consisting of one or more Roman numeral characters (`[ivxlcdmIVXLCDM]+`).
    public static final Pattern ROMAN_NUMERALS = Pattern.compile("[ivxlcdmIVXLCDM]+");

    /// Matches named, decimal, or hexadecimal HTML character entities (`&[#\p{Alnum}]+;`).
    public static final Pattern HTML_CHARACTER_ENTITY = Pattern.compile("&[#\\p{Alnum}]+;");

    /// Matches an opening curly brace that is not preceded by a backslash (`(?<!\\)\{`).
    public static final Pattern UNESCAPED_CURLY_BRACE_OPEN = Pattern.compile("(?<!\\\\)\\{");

    /// Matches a closing curly brace that is not preceded by a backslash (`(?<!\\)}`).
    public static final Pattern UNESCAPED_CURLY_BRACE_CLOSE = Pattern.compile("(?<!\\\\)}");

    private RegexPatterns() {
    }
}
