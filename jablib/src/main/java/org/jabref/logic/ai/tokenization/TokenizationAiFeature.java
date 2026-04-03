package org.jabref.logic.ai.tokenization;

import org.jabref.logic.ai.AiFeature;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.database.BibDatabaseContext;

public class TokenizationAiFeature implements AiFeature {
    private final CurrentTokenEstimator currentTokenEstimator;

    public TokenizationAiFeature(AiPreferences aiPreferences) {
        this.currentTokenEstimator = new CurrentTokenEstimator(aiPreferences);
    }

    @Override
    public void setupDatabase(BibDatabaseContext databaseContext) {
        // No listeners.
    }

    public CurrentTokenEstimator getCurrentTokenEstimator() {
        return currentTokenEstimator;
    }

    @Override
    public void close() throws Exception {
        // Nothing to close.
    }
}
