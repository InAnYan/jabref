package org.jabref.logic.ai.summarization.logic.summarizationalgorithms;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.ai.summarization.templates.SummarizationChunkSystemMessageTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkUserMessageTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineSystemMessageTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineUserMessageTemplate;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.chatting.ChatModelInfo;
import org.jabref.model.ai.summarization.SummarizationAlgorithmName;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkedSummarizationAlgorithm implements SummarizationAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedSummarizationAlgorithm.class);

    private static final int MAX_OVERLAP_SIZE_IN_CHARS = 100;
    private static final int CHAR_TOKEN_FACTOR = 4; // Means, every token is roughly 4 characters.

    private final SummarizationChunkSystemMessageTemplate summarizationChunkSystemMessageTemplate;
    private final SummarizationChunkUserMessageTemplate summarizationChunkUserMessageTemplate;
    private final SummarizationCombineSystemMessageTemplate summarizationCombineSystemMessageTemplate;
    private final SummarizationCombineUserMessageTemplate summarizationCombineUserMessageTemplate;

    public ChunkedSummarizationAlgorithm(
            SummarizationChunkSystemMessageTemplate summarizationChunkSystemMessageTemplate,
            SummarizationChunkUserMessageTemplate summarizationChunkUserMessageTemplate,
            SummarizationCombineSystemMessageTemplate summarizationCombineSystemMessageTemplate,
            SummarizationCombineUserMessageTemplate summarizationCombineUserMessageTemplate
    ) {
        this.summarizationChunkSystemMessageTemplate = summarizationChunkSystemMessageTemplate;
        this.summarizationChunkUserMessageTemplate = summarizationChunkUserMessageTemplate;
        this.summarizationCombineSystemMessageTemplate = summarizationCombineSystemMessageTemplate;
        this.summarizationCombineUserMessageTemplate = summarizationCombineUserMessageTemplate;
    }

    @Override
    public String summarize(
            ChatModelInfo chatModelInfo,
            LongTaskInfo longTaskInfo,
            String text
    ) throws InterruptedException {
        LOGGER.debug("Summarizing text ({} chars)", text.length());

        longTaskInfo.progressCounter().increaseWorkMax(1); // For the combination of summary chunks.

        DocumentSplitter documentSplitter = DocumentSplitters.recursive(
                chatModelInfo.contextWindowSize() - MAX_OVERLAP_SIZE_IN_CHARS * 2 - estimateTokenCount(summarizationChunkSystemMessageTemplate.getSource()),
                MAX_OVERLAP_SIZE_IN_CHARS
        );

        List<String> chunkSummaries = documentSplitter.split(new DefaultDocument(text)).stream().map(TextSegment::text).toList();

        LOGGER.debug("Text was split into {} chunks", chunkSummaries.size());

        int passes = 0;

        do
        {
            passes++;
            LOGGER.debug("Summarizing {} chunk (of {}", passes, chunkSummaries.size());

            longTaskInfo.progressCounter().increaseWorkMax(chunkSummaries.size());

            List<String> list = new ArrayList<>();

            for (String chunkSummary : chunkSummaries) {
                if (longTaskInfo.shutdownSignal().get()) {
                    throw new InterruptedException();
                }

                String systemMessage =summarizationChunkSystemMessageTemplate.render();
                String userMessage = summarizationChunkUserMessageTemplate.render(chunkSummary);

                LOGGER.debug("Sending request to AI provider to summarize a chunk");
                String chunk = chatModelInfo.chatModel().chat(List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(userMessage)
                )).aiMessage().text();
                LOGGER.debug("Chunk {} summary was generated successfully", passes);

                list.add(chunk);
                longTaskInfo.progressCounter().increaseWorkDone(1);
            }

            chunkSummaries = list;
        } while (estimateTokenCount(chunkSummaries) > chatModelInfo.contextWindowSize() - estimateTokenCount(summarizationCombineSystemMessageTemplate.getSource()));

        if (chunkSummaries.size() == 1) {
            longTaskInfo.progressCounter().increaseWorkDone(1); // No need to call LLM for combination of summary chunks.
            LOGGER.debug("BibEntrySummary of the text was generated successfully");
            return chunkSummaries.getFirst();
        }

        String systemMessage = summarizationCombineSystemMessageTemplate.render();
        String userMessage = summarizationCombineUserMessageTemplate.render(chunkSummaries);

        if (longTaskInfo.shutdownSignal().get()) {
            throw new InterruptedException();
        }

        LOGGER.debug("Sending request to AI provider to combine summary chunks");
        String result = chatModelInfo.chatModel().chat(List.of(
                new SystemMessage(systemMessage),
                new UserMessage(userMessage)
        )).aiMessage().text();
        LOGGER.debug("BibEntrySummary of the text was generated successfully");

        longTaskInfo.progressCounter().increaseWorkDone(1);
        return result;
    }

    @Override
    public SummarizationAlgorithmName getName() {
        return SummarizationAlgorithmName.CHUNKED;
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
