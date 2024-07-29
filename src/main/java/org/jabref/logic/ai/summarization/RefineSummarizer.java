package org.jabref.logic.ai.summarization;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.Map;
import java.util.stream.Stream;

// NOTE FOR RUSLAN: Algorithm is wrong.
public class RefineSummarizer implements Summarizer {
    private final Summarizer documentSummarizer;

    private final PromptTemplate refinePromptTemplate;
    private final String refineSummaryVariable;
    private final String refineDocumentVariable;

    public RefineSummarizer(Summarizer documentSummarizer, PromptTemplate refinePromptTemplate, String refineSummaryVariable, String refineDocumentVariable) {
        this.documentSummarizer = documentSummarizer;
        this.refinePromptTemplate = refinePromptTemplate;
        this.refineSummaryVariable = refineSummaryVariable;
        this.refineDocumentVariable = refineDocumentVariable;
    }


    @Override
    public String summarize(ChatLanguageModel chatLanguageModel, String text) {
        return documentSummarizer.summarize(chatLanguageModel, text);
    }

    @Override
    public String summarize(ChatLanguageModel chatLanguageModel, Stream<String> texts) {
        return texts.reduce("", (refineSummary, text) -> {
            String documentSummary = documentSummarizer.summarize(chatLanguageModel, text);
            Prompt refinePrompt = makeRefinePrompt(refineSummary, documentSummary);
            return chatLanguageModel.generate(refinePrompt.text());
        });
    }

    private Prompt makeRefinePrompt(String refineSummaryValue, String refineDocumentValue) {
        return refinePromptTemplate.apply(Map.of(refineSummaryVariable, refineSummaryValue, refineDocumentVariable, refineDocumentValue));
    }
}
