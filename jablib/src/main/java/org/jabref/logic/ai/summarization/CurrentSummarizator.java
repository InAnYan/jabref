package org.jabref.logic.ai.summarization;

import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.SummarizatorFactory;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.templates.AiTemplatesFactory;
import org.jabref.model.ai.summarization.SummarizatorKind;

import org.jspecify.annotations.Nullable;

public class CurrentSummarizator implements Summarizator {
    private final AiPreferences aiPreferences;
    private final SummarizatorFactory summarizatorFactory;

    @Nullable
    private Summarizator summarizator = null;

    public CurrentSummarizator(
            AiPreferences aiPreferences,
            AiTemplatesFactory aiTemplatesFactory
    ) {
        this.aiPreferences = aiPreferences;
        this.summarizatorFactory = new SummarizatorFactory(aiTemplatesFactory);

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
        summarizator = summarizatorFactory.create(aiPreferences.getSummarizatorKind());
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
