package org.jabref.logic.ai.templates;

import org.jabref.logic.ai.chatting.templates.ChattingSystemMessageAiTemplate;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.citationparsing.templates.CitationParsingSystemMessageAiTemplate;
import org.jabref.logic.ai.citationparsing.templates.CitationParsingUserMessageAiTemplate;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkUserMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineUserMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationFullDocumentSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationFullDocumentUserMessageAiTemplate;

public class CurrentAiTemplates implements AiTemplatesFactory {
    private final SummarizationChunkSystemMessageAiTemplate summarizationChunkSystemMessageTemplate;
    private final SummarizationChunkUserMessageAiTemplate summarizationChunkUserMessageTemplate;
    private final SummarizationCombineSystemMessageAiTemplate summarizationCombineSystemMessageTemplate;
    private final SummarizationCombineUserMessageAiTemplate summarizationCombineUserMessageTemplate;

    private final SummarizationFullDocumentSystemMessageAiTemplate summarizationFullDocumentSystemMessageTemplate;
    private final SummarizationFullDocumentUserMessageAiTemplate summarizationFullDocumentUserMessageTemplate;

    private final ChattingSystemMessageAiTemplate chattingSystemMessageTemplate;
    private final ChattingUserMessageAiTemplate chattingUserMessageTemplate;

    private final CitationParsingSystemMessageAiTemplate citationParsingSystemMessageTemplate;
    private final CitationParsingUserMessageAiTemplate citationParsingUserMessageTemplate;

    public CurrentAiTemplates(AiPreferences aiPreferences) {
        this.summarizationChunkSystemMessageTemplate = new SummarizationChunkSystemMessageAiTemplate(
                aiPreferences::getSummarizationChunkSystemMessageTemplate
        );
        this.summarizationChunkUserMessageTemplate = new SummarizationChunkUserMessageAiTemplate(
                aiPreferences::getSummarizationChunkUserMessageTemplate
        );
        this.summarizationCombineSystemMessageTemplate = new SummarizationCombineSystemMessageAiTemplate(
                aiPreferences::getSummarizationCombineSystemMessageTemplate
        );
        this.summarizationCombineUserMessageTemplate = new SummarizationCombineUserMessageAiTemplate(
                aiPreferences::getSummarizationCombineUserMessageTemplate
        );

        this.summarizationFullDocumentSystemMessageTemplate = new SummarizationFullDocumentSystemMessageAiTemplate(
                aiPreferences::getSummarizationFullDocumentSystemMessageTemplate
        );
        this.summarizationFullDocumentUserMessageTemplate = new SummarizationFullDocumentUserMessageAiTemplate(
                aiPreferences::getSummarizationFullDocumentUserMessageTemplate
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
        this.citationParsingUserMessageTemplate = new CitationParsingUserMessageAiTemplate(
                aiPreferences::getCitationParsingUserMessageTemplate
        );
    }

    @Override
    public SummarizationChunkSystemMessageAiTemplate getSummarizationChunkSystemMessageTemplate() {
        return summarizationChunkSystemMessageTemplate;
    }

    @Override
    public SummarizationChunkUserMessageAiTemplate getSummarizationChunkUserMessageTemplate() {
        return summarizationChunkUserMessageTemplate;
    }

    @Override
    public SummarizationCombineSystemMessageAiTemplate getSummarizationCombineSystemMessageTemplate() {
        return summarizationCombineSystemMessageTemplate;
    }

    @Override
    public SummarizationCombineUserMessageAiTemplate getSummarizationCombineUserMessageTemplate() {
        return summarizationCombineUserMessageTemplate;
    }

    @Override
    public SummarizationFullDocumentSystemMessageAiTemplate getSummarizationFullDocumentSystemMessageTemplate() {
        return summarizationFullDocumentSystemMessageTemplate;
    }

    @Override
    public SummarizationFullDocumentUserMessageAiTemplate getSummarizationFullDocumentUserMessageTemplate() {
        return summarizationFullDocumentUserMessageTemplate;
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

    @Override
    public CitationParsingUserMessageAiTemplate getCitationParsingUserMessageTemplate() {
        return citationParsingUserMessageTemplate;
    }
}
