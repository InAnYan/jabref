package org.jabref.logic.ai.templates;

import org.jabref.logic.ai.chatting.templates.ChattingSystemMessageAiTemplate;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.citationparsing.templates.CitationParsingSystemMessageAiTemplate;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationFullDocumentSystemMessageAiTemplate;

public class CurrentAiTemplates implements AiTemplatesFactory {
    private final SummarizationChunkSystemMessageAiTemplate summarizationChunkSystemMessageTemplate;
    private final SummarizationCombineSystemMessageAiTemplate summarizationCombineSystemMessageTemplate;
    private final SummarizationFullDocumentSystemMessageAiTemplate summarizationFullDocumentSystemMessageTemplate;

    private final ChattingSystemMessageAiTemplate chattingSystemMessageTemplate;
    private final ChattingUserMessageAiTemplate chattingUserMessageTemplate;

    private final CitationParsingSystemMessageAiTemplate citationParsingSystemMessageTemplate;

    public CurrentAiTemplates(AiPreferences aiPreferences) {
        this.summarizationChunkSystemMessageTemplate = new SummarizationChunkSystemMessageAiTemplate(
                aiPreferences::getSummarizationChunkSystemMessageTemplate
        );
        this.summarizationCombineSystemMessageTemplate = new SummarizationCombineSystemMessageAiTemplate(
                aiPreferences::getSummarizationCombineSystemMessageTemplate
        );
        this.summarizationFullDocumentSystemMessageTemplate = new SummarizationFullDocumentSystemMessageAiTemplate(
                aiPreferences::getSummarizationFullDocumentSystemMessageTemplate
        );

        this.chattingSystemMessageTemplate = new ChattingSystemMessageAiTemplate(
                aiPreferences::getChattingSystemMessageTemplate
        );
        this.chattingUserMessageTemplate = new ChattingUserMessageAiTemplate(
                aiPreferences::getChattingUserMessageTemplate
        );

        this.citationParsingSystemMessageTemplate = new CitationParsingSystemMessageAiTemplate(
                aiPreferences::getCitationParsingSystemMessageTemplate
        );
    }

    @Override
    public SummarizationChunkSystemMessageAiTemplate getSummarizationChunkSystemMessageTemplate() {
        return summarizationChunkSystemMessageTemplate;
    }

    @Override
    public SummarizationCombineSystemMessageAiTemplate getSummarizationCombineSystemMessageTemplate() {
        return summarizationCombineSystemMessageTemplate;
    }

    @Override
    public SummarizationFullDocumentSystemMessageAiTemplate getSummarizationFullDocumentSystemMessageTemplate() {
        return summarizationFullDocumentSystemMessageTemplate;
    }

    @Override
    public ChattingSystemMessageAiTemplate getChattingSystemMessageTemplate() {
        return chattingSystemMessageTemplate;
    }

    @Override
    public ChattingUserMessageAiTemplate getChattingUserMessageTemplate() {
        return chattingUserMessageTemplate;
    }

    @Override
    public CitationParsingSystemMessageAiTemplate getCitationParsingSystemMessageTemplate() {
        return citationParsingSystemMessageTemplate;
    }
}
