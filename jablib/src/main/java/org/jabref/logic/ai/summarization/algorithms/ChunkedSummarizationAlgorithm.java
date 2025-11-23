package org.jabref.logic.ai.summarization.algorithms;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.ai.templating.AiTemplate;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkedSummarizationAlgorithm implements SummarizationAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedSummarizationAlgorithm.class);

    private static final int MAX_OVERLAP_SIZE_IN_CHARS = 100;
    private static final int CHAR_TOKEN_FACTOR = 4; // Means, every token is roughly 4 characters.

    // TODO: AiPreferences are used here to determine the context window size of the chat model, but this is wrong,
    // because AiPreferences determine the size based on the selected model, but this model can be supplied with other model.
    private final AiPreferences aiPreferences;
    private final AiTemplatesService aiTemplatesService;
    private final ChatModel chatModel;

    public ChunkedSummarizationAlgorithm(
            AiPreferences aiPreferences,
            AiTemplatesService aiTemplatesService,
            ChatModel chatModel
    ) {
        this.aiPreferences = aiPreferences;
        this.aiTemplatesService = aiTemplatesService;
        this.chatModel = chatModel;
    }

    @Override
    public String summarize(String text, ProgressCounter progressCounter, ReadOnlyBooleanProperty shutdownSignal) throws InterruptedException {
        LOGGER.debug("Summarizing text ({} chars)", text.length());

        progressCounter.increaseWorkMax(1); // For the combination of summary chunks.

        DocumentSplitter documentSplitter = DocumentSplitters.recursive(
                aiPreferences.getContextWindowSize() - MAX_OVERLAP_SIZE_IN_CHARS * 2 - estimateTokenCount(aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE)),
                MAX_OVERLAP_SIZE_IN_CHARS
        );

        List<String> chunkSummaries = documentSplitter.split(new DefaultDocument(text)).stream().map(TextSegment::text).toList();

        LOGGER.debug("Text was split into {} chunks", chunkSummaries.size());

        int passes = 0;

        do
        {
            passes++;
            LOGGER.debug("Summarizing {} chunk (of {}", passes, chunkSummaries.size());

            progressCounter.increaseWorkMax(chunkSummaries.size());

            List<String> list = new ArrayList<>();

            for (String chunkSummary : chunkSummaries) {
                if (shutdownSignal.get()) {
                    throw new InterruptedException();
                }

                String systemMessage = aiTemplatesService.makeSummarizationChunkSystemMessage();
                String userMessage = aiTemplatesService.makeSummarizationChunkUserMessage(chunkSummary);

                LOGGER.debug("Sending request to AI provider to summarize a chunk");
                String chunk = chatModel.chat(List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(userMessage)
                )).aiMessage().text();
                LOGGER.debug("Chunk {} summary was generated successfully", passes);

                list.add(chunk);
                progressCounter.increaseWorkDone(1);
            }

            chunkSummaries = list;
        } while (estimateTokenCount(chunkSummaries) > aiPreferences.getContextWindowSize() - estimateTokenCount(aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE)));

        if (chunkSummaries.size() == 1) {
            progressCounter.increaseWorkDone(1); // No need to call LLM for combination of summary chunks.
            LOGGER.debug("Summary of the text was generated successfully");
            return chunkSummaries.getFirst();
        }

        String systemMessage = aiTemplatesService.makeSummarizationCombineSystemMessage();
        String userMessage = aiTemplatesService.makeSummarizationCombineUserMessage(chunkSummaries);

        if (shutdownSignal.get()) {
            throw new InterruptedException();
        }

        LOGGER.debug("Sending request to AI provider to combine summary chunks");
        String result = chatModel.chat(List.of(
                new SystemMessage(systemMessage),
                new UserMessage(userMessage)
        )).aiMessage().text();
        LOGGER.debug("Summary of the text was generated successfully");

        progressCounter.increaseWorkDone(1);
        return result;
    }

    private static int estimateTokenCount(List<String> chunkSummaries) {
        return chunkSummaries.stream().mapToInt(ChunkedSummarizationAlgorithm::estimateTokenCount).sum();
    }

    private static int estimateTokenCount(String string) {
        return estimateTokenCount(string.length());
    }

    private static int estimateTokenCount(int numOfChars) {
        return numOfChars / CHAR_TOKEN_FACTOR;
    }
}
