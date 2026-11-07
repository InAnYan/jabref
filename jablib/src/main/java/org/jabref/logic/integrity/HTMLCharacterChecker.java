package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.RegexPatterns;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldProperty;

/// Checks, if there are any HTML encoded characters in non-verbatim fields.
public class HTMLCharacterChecker implements EntryChecker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return entry.getFieldMap().entrySet().stream()
                    .filter(field -> !field.getKey().getProperties().contains(FieldProperty.VERBATIM))
                    .filter(field -> RegexPatterns.HTML_CHARACTER_ENTITY.matcher(field.getValue()).find())
                    .map(field -> new IntegrityMessage(Localization.lang("HTML encoded character found"), entry, field.getKey()))
                    .toList();
    }
}
