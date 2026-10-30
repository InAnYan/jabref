package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.RegexPatterns;

import org.jspecify.annotations.NonNull;

/// Removes all hyphenated line breaks in the string.
public class RemoveHyphenatedNewlinesFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Remove hyphenated line breaks");
    }

    @Override
    public String getKey() {
        return "remove_hyphenated_newlines";
    }

    @Override
    public String format(@NonNull String value) {
        value = RegexPatterns.HYPHENATED_LINE_BREAK.matcher(value).replaceAll("");
        return value.trim();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Removes all hyphenated line breaks in the field content.");
    }

    @Override
    public String getExampleInput() {
        return "Gimme shel-\nter";
    }
}
