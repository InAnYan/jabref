package org.jabref.logic.ai.summarization.logic.summarizationalgorithms;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.summarization.SummarizatorKind;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->feat~ai.summarization.algorithms.chunked~1]
// [impl->req~ai.summarization.general.unlimited-size~1]
public class ChunkedSummarizator implements Summarizator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedSummarizator.class);

    // TODO: Make a parameter?
    private static final int MAX_OVERLAP_SIZE_IN_CHARS = 100;

    private final String summarizationChunkSystemMessageTemplate;
    private final String summarizationCombineSystemMessageTemplate;

    public ChunkedSummarizator(
            String summarizationChunkSystemMessageTemplate,
            String summarizationCombineSystemMessageTemplate
    ) {
        this.summarizationChunkSystemMessageTemplate = summarizationChunkSystemMessageTemplate;
        this.summarizationCombineSystemMessageTemplate = summarizationCombineSystemMessageTemplate;
    }

    @Override
    public String summarize(
            ChatModel chatModel,
            String text
    ) throws InterruptedException {
        // TODO: Simplify.
        LOGGER.debug("Summarizing text ({} chars)", text.length());

        DocumentSplitter documentSplitter = DocumentSplitters.recursive(
                chatModel.getContextWindowSize() - MAX_OVERLAP_SIZE_IN_CHARS * 2 - chatModel.getTokenizer().estimate(
                        ChatMessage.Role.SYSTEM,
                        summarizationChunkSystemMessageTemplate
                ),
                MAX_OVERLAP_SIZE_IN_CHARS
        );

        List<String> chunkSummaries = documentSplitter.split(new DefaultDocument(text)).stream().map(TextSegment::text).toList();

        LOGGER.debug("Text was split into {} chunks", chunkSummaries.size());

        int passes = 0;

        do {
            passes++;
            LOGGER.debug("Summarizing {} chunk (of {}", passes, chunkSummaries.size());

            List<String> list = new ArrayList<>();

            for (String chunkSummary : chunkSummaries) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                String systemMessage = AiTemplateRenderer.renderSummarizationChunkSystemMessage(summarizationChunkSystemMessageTemplate);

                LOGGER.debug("Sending request to AI provider to summarize a chunk");
                String chunk = chatModel.chat(List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(chunkSummary)
                )).aiMessage().text();
                LOGGER.debug("Chunk {} summary was generated successfully", passes);

                list.add(chunk);
            }

            chunkSummaries = list;
        } while (chatModel.getTokenizer().estimate(chunkSummaries.stream().map(ChatMessage::userMessage).toList()) > chatModel.getContextWindowSize() - chatModel.getTokenizer().estimate(
                ChatMessage.Role.SYSTEM,
                summarizationCombineSystemMessageTemplate
        ));

        if (chunkSummaries.size() == 1) {
            LOGGER.debug("BibEntrySummary of the text was generated successfully");
            return chunkSummaries.getFirst();
        }

        String systemMessage = AiTemplateRenderer.renderSummarizationCombineSystemMessage(summarizationCombineSystemMessageTemplate);

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        LOGGER.debug("Sending request to AI provider to combine summary chunks");
        String result = chatModel.chat(List.of(
                new SystemMessage(systemMessage),
                new UserMessage(String.join("\n\n", chunkSummaries))
        )).aiMessage().text();
        LOGGER.debug("BibEntrySummary of the text was generated successfully");
        return result;
    }

    @Override
    public SummarizatorKind getKind() {
        return SummarizatorKind.CHUNKED;
    }
}
