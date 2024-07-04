package org.jabref.logic.ai.summarization;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Summarizer {
    default String summarize(ChatLanguageModel chatLanguageModel, Document document) {
        return summarize(chatLanguageModel, document.text());
    }

    default String summarize(ChatLanguageModel chatLanguageModel, TextSegment textSegment) {
        return summarize(chatLanguageModel, textSegment.text());
    }

    default String summarizeAll(ChatLanguageModel chatLanguageModel, Stream<Document> documents) {
        return summarize(chatLanguageModel, documents.map(Document::text));
    }

    default String summarize(ChatLanguageModel chatLanguageModel, Stream<String> text) {
        return summarize(chatLanguageModel, text.collect(Collectors.joining("\n")));
    }

    String summarize(ChatLanguageModel chatLanguageModel, String text);
}
