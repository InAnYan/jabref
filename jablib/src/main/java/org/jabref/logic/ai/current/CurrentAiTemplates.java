package org.jabref.logic.ai.current;

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
import org.jabref.logic.ai.templates.AiTemplatesFactory;
import org.jabref.model.ai.templating.AiTemplateKind;

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
                () -> aiPreferences.getTemplate(AiTemplateKind.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE)
        );
        this.summarizationChunkUserMessageTemplate = new SummarizationChunkUserMessageAiTemplate(
                () -> aiPreferences.getTemplate(AiTemplateKind.SUMMARIZATION_CHUNK_USER_MESSAGE)
        );
        this.summarizationCombineSystemMessageTemplate = new SummarizationCombineSystemMessageAiTemplate(
                () -> aiPreferences.getTemplate(AiTemplateKind.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE)
        );
        this.summarizationCombineUserMessageTemplate = new SummarizationCombineUserMessageAiTemplate(
                () -> aiPreferences.getTemplate(AiTemplateKind.SUMMARIZATION_CHUNK_USER_MESSAGE)
        );

        this.summarizationFullDocumentSystemMessageTemplate = new SummarizationFullDocumentSystemMessageAiTemplate(
                () -> aiPreferences.getTemplate(AiTemplateKind.SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE)
        );
        this.summarizationFullDocumentUserMessageTemplate = new SummarizationFullDocumentUserMessageAiTemplate(
                () -> aiPreferences.getTemplate(AiTemplateKind.SUMMARIZATION_FULL_DOCUMENT_USER_MESSAGE)
        );

        this.chattingSystemMessageTemplate = new ChattingSystemMessageAiTemplate(
                () -> aiPreferences.getTemplate(AiTemplateKind.CHATTING_SYSTEM_MESSAGE)
        );
        this.chattingUserMessageTemplate = new ChattingUserMessageAiTemplate(
                () -> aiPreferences.getTemplate(AiTemplateKind.CHATTING_USER_MESSAGE)
        );

        this.citationParsingSystemMessageTemplate = new CitationParsingSystemMessageAiTemplate(
                () -> aiPreferences.getTemplate(AiTemplateKind.CITATION_PARSING_SYSTEM_MESSAGE)
        );
        this.citationParsingUserMessageTemplate = new CitationParsingUserMessageAiTemplate(
                () -> aiPreferences.getTemplate(AiTemplateKind.CITATION_PARSING_USER_MESSAGE)
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
