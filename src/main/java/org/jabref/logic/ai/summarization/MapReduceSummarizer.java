package org.jabref.logic.ai.summarization;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapReduceSummarizer implements Summarizer {
    private final Summarizer chunkSummarizer;
    private final Summarizer combineSummarizer;

    private final int chunkSize;
    private final DocumentSplitter documentSplitter;


    public MapReduceSummarizer(Summarizer chunkSummarizer,
                               Summarizer combineSummarizer,
                               int chunkSize,
                               int chunkOverlapSize) {
        this.chunkSummarizer = chunkSummarizer;
        this.combineSummarizer = combineSummarizer;
        this.chunkSize = chunkSize;
        this.documentSplitter = DocumentSplitters.recursive(chunkSize, chunkOverlapSize);
    }


    @Override
    public String summarize(ChatLanguageModel chatLanguageModel, String text) {
        // This algorithm has several implicit assumptions:
        // 1. Text will always shrink or will shrink in a deterministic time (that should be achieved through a careful prompt).
        // 2. Chunk size is not negative.

        while (text.length() > chunkSize) {
            text = splitString(text).map(chunk -> chunkSummarizer.summarize(chatLanguageModel, chunk)).collect(Collectors.joining("\n"));
        }

        return combineSummarizer.summarize(chatLanguageModel, text);
    }

    private Stream<String> splitString(String text) {
        return documentSplitter.split(new Document(text)).stream().map(TextSegment::text);
    }
}
