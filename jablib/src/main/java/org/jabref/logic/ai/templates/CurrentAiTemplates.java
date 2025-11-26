package org.jabref.logic.ai.templates;

import org.jabref.logic.ai.chatting.templates.ChattingSystemMessageTemplate;
import org.jabref.logic.ai.chatting.templates.ChattingUserMessageTemplate;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkSystemMessageTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationChunkUserMessageTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineSystemMessageTemplate;
import org.jabref.logic.ai.summarization.templates.SummarizationCombineUserMessageTemplate;
import org.jabref.model.ai.templating.AiTemplate;

public class CurrentAiTemplates {
    private final SummarizationChunkSystemMessageTemplate summarizationChunkSystemMessageTemplate;
    private final SummarizationChunkUserMessageTemplate summarizationChunkUserMessageTemplate;
    private final SummarizationCombineSystemMessageTemplate summarizationCombineSystemMessageTemplate;
    private final SummarizationCombineUserMessageTemplate summarizationCombineUserMessageTemplate;

    private final ChattingSystemMessageTemplate chattingSystemMessageTemplate;
    private final ChattingUserMessageTemplate chattingUserMessageTemplate;

    public CurrentAiTemplates(AiPreferences aiPreferences) {
        this.summarizationChunkSystemMessageTemplate = new SummarizationChunkSystemMessageTemplate(
                () -> aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE)
        );
        this.summarizationChunkUserMessageTemplate = new SummarizationChunkUserMessageTemplate(
                () -> aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE)
        );
        this.summarizationCombineSystemMessageTemplate = new SummarizationCombineSystemMessageTemplate(
                () -> aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE)
        );
        this.summarizationCombineUserMessageTemplate = new SummarizationCombineUserMessageTemplate(
                () -> aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE)
        );

        this.chattingSystemMessageTemplate = new ChattingSystemMessageTemplate(
                () -> aiPreferences.getTemplate(AiTemplate.CHATTING_SYSTEM_MESSAGE)
        );
        this.chattingUserMessageTemplate = new ChattingUserMessageTemplate(
                () -> aiPreferences.getTemplate(AiTemplate.CHATTING_USER_MESSAGE)
        );
    }

    public SummarizationChunkSystemMessageTemplate getSummarizationChunkSystemMessageTemplate() {
        return summarizationChunkSystemMessageTemplate;
    }

    public SummarizationChunkUserMessageTemplate getSummarizationChunkUserMessageTemplate() {
        return summarizationChunkUserMessageTemplate;
    }

    public SummarizationCombineSystemMessageTemplate getSummarizationCombineSystemMessageTemplate() {
        return summarizationCombineSystemMessageTemplate;
    }

    public SummarizationCombineUserMessageTemplate getSummarizationCombineUserMessageTemplate() {
        return summarizationCombineUserMessageTemplate;
    }

    public ChattingSystemMessageTemplate getChattingSystemMessageTemplate() {
        return chattingSystemMessageTemplate;
    }

    public ChattingUserMessageTemplate getChattingUserMessageTemplate() {
        return chattingUserMessageTemplate;
    }
}
