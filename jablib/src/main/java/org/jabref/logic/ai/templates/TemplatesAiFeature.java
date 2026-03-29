package org.jabref.logic.ai.templates;

import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.preferences.AiPreferences;

public class TemplatesAiFeature extends AiFeature {
    private final CurrentAiTemplates currentAiTemplates;

    public TemplatesAiFeature(AiPreferences aiPreferences) {
        this.currentAiTemplates = new CurrentAiTemplates(aiPreferences);
    }


    public AiTemplatesFactory getCurrentAiTemplates() {
        return currentAiTemplates;
    }

    @Override
    public void close() throws Exception {
        // Nothing to close.
    }
}
