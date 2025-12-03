package org.jabref.logic.ai.templates;

import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.database.BibDatabaseContext;

public class TemplatesAiFeature implements AiFeature {
    private final CurrentAiTemplates currentAiTemplates;

    public TemplatesAiFeature(AiPreferences aiPreferences) {
        this.currentAiTemplates = new CurrentAiTemplates(aiPreferences);
    }

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // Nothing to listen for.
    }

    public AiTemplatesFactory getCurrentAiTemplates() {
        return currentAiTemplates;
    }

    @Override
    public void close() throws Exception {
        // Nothing to close.
    }
}
