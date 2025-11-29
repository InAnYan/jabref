package org.jabref.model.ai.chatting.messages;

import java.time.Instant;
import java.util.List;

import org.jabref.model.ai.chatting.content.MessageContent;
import org.jabref.model.ai.pipeline.RelevantInformation;

public class LlmMessage extends JabRefChatMessage {
    private final List<MessageContent> detailedContent;
    private final List<RelevantInformation> relevantInformation;

    public LlmMessage(
            Instant createdAt,
            String content,
            List<MessageContent> detailedContent,
            List<RelevantInformation> relevantInformation
    ) {
        super(createdAt, content);

        this.detailedContent = detailedContent;
        this.relevantInformation = relevantInformation;
    }

    public List<MessageContent> getDetailedContent() {
        return detailedContent;
    }

    public List<RelevantInformation> getRelevantInformation() {
        return relevantInformation;
    }

    @Override
    public JabRefChatMessageType getType() {
        return JabRefChatMessageType.USER_MESSAGE;
    }
}
