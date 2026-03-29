package org.jabref.logic.ai.summarization.logic.summarizationalgorithms;

import java.util.List;

import org.jabref.logic.ai.customimplementations.llms.ChatModel;
import org.jabref.logic.ai.summarization.templates.SummarizationFullDocumentSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationFullDocumentUserMessageAiTemplate;
import org.jabref.model.ai.summarization.SummarizatorKind;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullDocumentSummarizator implements Summarizator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullDocumentSummarizator.class);

    private final SummarizationFullDocumentSystemMessageAiTemplate systemTemplate;
    private final SummarizationFullDocumentUserMessageAiTemplate userTemplate;

    public FullDocumentSummarizator(
            SummarizationFullDocumentSystemMessageAiTemplate systemTemplate,
            SummarizationFullDocumentUserMessageAiTemplate userTemplate
    ) {
        this.systemTemplate = systemTemplate;
        this.userTemplate = userTemplate;
    }

    @Override
    public String summarize(ChatModel chatModel, String text) throws InterruptedException {
        LOGGER.debug("Summarizing whole document ({} chars)", text.length());

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        String systemMessage = systemTemplate.render();
        String userMessage = userTemplate.render(text);

        LOGGER.debug("Sending request to AI provider to summarize the full document");
        String result = chatModel.chat(List.of(
                new SystemMessage(systemMessage),
                new UserMessage(userMessage)
        )).aiMessage().text();
        
        LOGGER.debug("Full-document summary was generated successfully");
        return result;
    }

    @Override
    public SummarizatorKind getKind() {
        return SummarizatorKind.FULL_DOCUMENT;
    }
}
