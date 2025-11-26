package org.jabref.logic.ai.summarization;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.ChunkedSummarizationAlgorithm;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.SummarizationAlgorithm;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkSystemMessageTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkUserMessageTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineSystemMessageTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineUserMessageTemplate;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.chatting.ChatModelInfo;
import org.jabref.model.ai.summarization.SummarizationAlgorithmName;
import org.jabref.model.ai.templating.AiTemplate;

import org.jspecify.annotations.Nullable;

public class CurrentlySelectedSummarizationAlgorithm implements SummarizationAlgorithm {
    private final AiPreferences aiPreferences;

    @Nullable
    private SummarizationAlgorithm summarizationAlgorithm = null;

    public CurrentlySelectedSummarizationAlgorithm(
            AiPreferences aiPreferences
    ) {
        this.aiPreferences = aiPreferences;

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
        //noinspection SwitchStatementWithTooFewBranches
        switch (aiPreferences.getDefaultSummarizationAlgorithm()) {
            case SummarizationAlgorithmName.CHUNKED -> {
                summarizationAlgorithm = createChunkedSummarizationAlgorithm();
            }
        }
    }

    private ChunkedSummarizationAlgorithm createChunkedSummarizationAlgorithm() {
        return new ChunkedSummarizationAlgorithm(
                new SummarizationChunkSystemMessageTemplate(
                        aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE)
                ),
                new SummarizationChunkUserMessageTemplate(
                        aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE)
                ),
                new SummarizationCombineSystemMessageTemplate(
                        aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE)
                ),
                new SummarizationCombineUserMessageTemplate(
                        aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE)
                )
        );
    }

    @Override
    public String summarize(ChatModelInfo chatModelInfo, LongTaskInfo longTaskInfo, String text) throws InterruptedException {
        if (summarizationAlgorithm == null) {
            return "";
        }

        return summarizationAlgorithm.summarize(chatModelInfo, longTaskInfo, text);
    }

    @Override
    public SummarizationAlgorithmName getName() {
        if (summarizationAlgorithm == null) {
            // Sadly.
            return null;
        }

        return summarizationAlgorithm.getName();
    }
}
