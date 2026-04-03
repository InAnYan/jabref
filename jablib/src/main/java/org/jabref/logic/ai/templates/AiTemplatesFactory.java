package org.jabref.logic.ai.templates;

import org.jabref.logic.ai.chatting.templates.ChattingSystemMessageAiTemplate;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageAiTemplate;
import org.jabref.logic.ai.citationparsing.templates.CitationParsingSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineSystemMessageAiTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationFullDocumentSystemMessageAiTemplate;

public interface AiTemplatesFactory {
    SummarizationChunkSystemMessageAiTemplate getSummarizationChunkSystemMessageTemplate();

    SummarizationCombineSystemMessageAiTemplate getSummarizationCombineSystemMessageTemplate();

    SummarizationFullDocumentSystemMessageAiTemplate getSummarizationFullDocumentSystemMessageTemplate();

    CitationParsingSystemMessageAiTemplate getCitationParsingSystemMessageTemplate();

    ChattingSystemMessageAiTemplate getChattingSystemMessageTemplate();

    ChattingUserMessageAiTemplate getChattingUserMessageTemplate();
}
