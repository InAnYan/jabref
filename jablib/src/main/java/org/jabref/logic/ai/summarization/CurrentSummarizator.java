package org.jabref.logic.ai.summarization;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.SummarizatorFactory;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.ai.summarization.SummarizatorKind;

import org.jspecify.annotations.Nullable;

public class CurrentSummarizator implements Summarizator {
    private final AiPreferences aiPreferences;

    @Nullable
    private Summarizator summarizator = null;

    public CurrentSummarizator(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        updateAlgorithm();
        setupListeningToPreferences();
    }

    private void setupListeningToPreferences() {
        aiPreferences.summarizatorKindProperty().addListener(_ -> updateAlgorithm());

        // Not efficient.
        aiPreferences.summarizationChunkSystemMessageTemplateProperty().addListener(_ -> updateAlgorithm());
        aiPreferences.summarizationChunkUserMessageTemplateProperty().addListener(_ -> updateAlgorithm());
        aiPreferences.summarizationCombineSystemMessageTemplateProperty().addListener(_ -> updateAlgorithm());
        aiPreferences.summarizationCombineUserMessageTemplateProperty().addListener(_ -> updateAlgorithm());
        aiPreferences.summarizationFullDocumentSystemMessageTemplateProperty().addListener(_ -> updateAlgorithm());
        aiPreferences.summarizationFullDocumentUserMessageTemplateProperty().addListener(_ -> updateAlgorithm());
    }

    private void updateAlgorithm() {
        summarizator = SummarizatorFactory.create(
                aiPreferences.getSummarizatorKind(),
                aiPreferences.getSummarizationChunkSystemMessageTemplate(),
                aiPreferences.getSummarizationCombineSystemMessageTemplate(),
                aiPreferences.getSummarizationFullDocumentSystemMessageTemplate()
        );
    }

    @Override
    public String summarize(ChatModel chatModel, String text) throws InterruptedException {
        if (summarizator == null) {
            throw new RuntimeException("No summarization algorithm selected.");
        }

        return summarizator.summarize(chatModel, text);
    }

    @Override
    public SummarizatorKind getKind() {
        return aiPreferences.getSummarizatorKind();
    }
}
