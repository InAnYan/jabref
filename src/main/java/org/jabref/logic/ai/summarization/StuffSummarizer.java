package org.jabref.logic.ai.summarization;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.Collections;

public class StuffSummarizer implements Summarizer {
    private final PromptTemplate promptTemplate;
    private final String textVariableName;

    public StuffSummarizer(PromptTemplate promptTemplate, String textVariableName) {
        this.promptTemplate = promptTemplate;
        this.textVariableName = textVariableName;
    }

    @Override
    public String summarize(ChatLanguageModel chatLanguageModel, String text) {
        Prompt prompt = promptTemplate.apply(Collections.singletonMap(textVariableName, text));
        return chatLanguageModel.generate(prompt.text());
    }
}
