package org.jabref.logic.ai.current;

import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.SummarizatorFactory;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.templates.AiTemplatesFactory;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.templating.AiTemplateKind;

import org.jspecify.annotations.Nullable;

public class CurrentSummarizator implements Summarizator {
    private final AiPreferences aiPreferences;
    private final AiTemplatesFactory aiTemplatesFactory;

    @Nullable
    private Summarizator summarizator = null;

    public CurrentSummarizator(
            AiPreferences aiPreferences,
            AiTemplatesFactory aiTemplatesFactory
    ) {
        this.aiPreferences = aiPreferences;
        this.aiTemplatesFactory = aiTemplatesFactory;

        updateAlgorithm();
        setupListeningToPreferences();
    }

    private void setupListeningToPreferences() {
        aiPreferences.summarizatorKindProperty().addListener(_ -> updateAlgorithm());

        // Not efficient.
        aiPreferences.templateProperty(AiTemplateKind.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE).addListener(_ -> updateAlgorithm());
        aiPreferences.templateProperty(AiTemplateKind.SUMMARIZATION_CHUNK_USER_MESSAGE).addListener(_ -> updateAlgorithm());
        aiPreferences.templateProperty(AiTemplateKind.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE).addListener(_ -> updateAlgorithm());
        aiPreferences.templateProperty(AiTemplateKind.SUMMARIZATION_COMBINE_USER_MESSAGE).addListener(_ -> updateAlgorithm());
        aiPreferences.templateProperty(AiTemplateKind.SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE).addListener(_ -> updateAlgorithm());
        aiPreferences.templateProperty(AiTemplateKind.SUMMARIZATION_FULL_DOCUMENT_USER_MESSAGE).addListener(_ -> updateAlgorithm());
    }

    private void updateAlgorithm() {
        summarizator = SummarizatorFactory.createSummarizator(aiTemplatesFactory, aiPreferences.getSummarizatorKind());
    }

    @Override
    public String summarize(ChatModel chatModel, LongTaskInfo longTaskInfo, String text) throws InterruptedException {
        if (summarizator == null) {
            throw new RuntimeException("No summarization algorithm selected.");
        }

        return summarizator.summarize(chatModel, longTaskInfo, text);
    }

    @Override
    public SummarizatorKind getKind() {
        return aiPreferences.getSummarizatorKind();
    }
}
