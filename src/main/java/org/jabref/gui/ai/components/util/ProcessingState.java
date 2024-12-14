package org.jabref.gui.ai.components.util;

import org.jabref.logic.l10n.Localization;

public enum ProcessingState {
    PROCESSING,
    SUCCESS,
    ERROR;

    public String toLocalizedString() {
        return switch (this) {
            case PROCESSING -> Localization.lang("Processing...");
            case SUCCESS -> Localization.lang("Success");
            case ERROR -> Localization.lang("Error");
        };
    }
}
