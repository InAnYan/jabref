package org.jabref.logic.ai.templates;

import org.jabref.logic.ai.chatting.templates.ChattingSystemMessageAiTemplate;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.citationparsing.templates.CitationParsingSystemMessageAiTemplate;
import org.jabref.logic.ai.citationparsing.templates.CitationParsingUserMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkUserMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineUserMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationFullDocumentSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationFullDocumentUserMessageAiTemplate;

public interface AiTemplatesFactory {
    SummarizationChunkSystemMessageAiTemplate getSummarizationChunkSystemMessageTemplate();
    SummarizationChunkUserMessageAiTemplate getSummarizationChunkUserMessageTemplate();
    SummarizationCombineSystemMessageAiTemplate getSummarizationCombineSystemMessageTemplate();
    SummarizationCombineUserMessageAiTemplate getSummarizationCombineUserMessageTemplate();
    SummarizationFullDocumentSystemMessageAiTemplate getSummarizationFullDocumentSystemMessageTemplate();
    SummarizationFullDocumentUserMessageAiTemplate getSummarizationFullDocumentUserMessageTemplate();

    CitationParsingSystemMessageAiTemplate getCitationParsingSystemMessageTemplate();
    CitationParsingUserMessageAiTemplate getCitationParsingUserMessageTemplate();

    ChattingSystemMessageAiTemplate getChattingSystemMessageTemplate();
    ChattingUserMessageAiTemplate getChattingUserMessageTemplate();
}
