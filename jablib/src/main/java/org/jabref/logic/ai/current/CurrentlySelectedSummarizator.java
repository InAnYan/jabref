package org.jabref.logic.ai.current;

import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.ChunkedSummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.templating.AiTemplate;

import org.jspecify.annotations.Nullable;

public class CurrentlySelectedSummarizator implements Summarizator {
    private final AiPreferences aiPreferences;
    private final CurrentAiTemplates currentAiTemplates;

    @Nullable
    private Summarizator summarizator = null;

    public CurrentlySelectedSummarizator(
            AiPreferences aiPreferences,
            CurrentAiTemplates currentAiTemplates
    ) {
        this.aiPreferences = aiPreferences;
        this.currentAiTemplates = currentAiTemplates;

        updateAlgorithm();
        setupListeningToPreferences();
    }

    private void setupListeningToPreferences() {
        aiPreferences.defaultSummarizationAlgorithmProperty().addListener(_ -> {
            updateAlgorithm();
        });

        // Not efficient.
        aiPreferences.templateProperty(AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE).addListener(_ -> {
            updateAlgorithm();
        });
        aiPreferences.templateProperty(AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE).addListener(_ -> {
            updateAlgorithm();
        });
        aiPreferences.templateProperty(AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE).addListener(_ -> {
            updateAlgorithm();
        });
        aiPreferences.templateProperty(AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE).addListener(_ -> {
            updateAlgorithm();
        });
    }

    private void updateAlgorithm() {
        // Because in the future there will be more strategies.
        //noinspection SwitchStatementWithTooFewBranches
        switch (aiPreferences.getDefaultSummarizationAlgorithm()) {
            case SummarizatorKind.CHUNKED -> {
                summarizator = createChunkedSummarizationAlgorithm();
            }
        }
    }

    private ChunkedSummarizator createChunkedSummarizationAlgorithm() {
        return new ChunkedSummarizator(
                currentAiTemplates.getSummarizationChunkSystemMessageTemplate(),
                currentAiTemplates.getSummarizationChunkUserMessageTemplate(),
                currentAiTemplates.getSummarizationCombineSystemMessageTemplate(),
                currentAiTemplates.getSummarizationCombineUserMessageTemplate()
        );
    }

    @Override
    public String summarize(ChatModel chatModel, LongTaskInfo longTaskInfo, String text) throws InterruptedException {
        if (summarizator == null) {
            throw new RuntimeException("No summarization algorithm selected.");
        }

        return summarizator.summarize(chatModel, longTaskInfo, text);
    }

    @Override
    public SummarizatorKind getName() {
        if (summarizator == null) {
            // Sadly.
            return null;
        }

        return summarizator.getName();
    }
}
