package org.jabref.model.ai.chatting.content;

public class ThinkingContent extends MessageContent {
    public ThinkingContent(String content) {
        super(content);
    }

    @Override
    public MessageContentType getType() {
        return MessageContentType.THINKING;
    }
}
