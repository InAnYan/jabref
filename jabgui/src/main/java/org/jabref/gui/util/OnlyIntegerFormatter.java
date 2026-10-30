package org.jabref.gui.util;

import java.util.function.UnaryOperator;

import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

import org.jabref.logic.util.strings.RegexPatterns;

/// Formatter that only accepts integer.
///
/// More or less taken from http://stackoverflow.com/a/36749659/873661
public class OnlyIntegerFormatter extends TextFormatter<Integer> {

    public OnlyIntegerFormatter() {
        this(0);
    }

    public OnlyIntegerFormatter(Integer defaultValue) {
        super(new IntegerStringConverter(), defaultValue, new IntegerFilter());
    }

    private static class IntegerFilter implements UnaryOperator<Change> {

        @Override
        public Change apply(TextFormatter.Change aT) {
            return RegexPatterns.ZERO_OR_MORE_DIGITS.matcher(aT.getText()).matches() ? aT : null;
        }
    }
}
