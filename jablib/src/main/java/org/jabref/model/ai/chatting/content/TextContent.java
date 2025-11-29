package org.jabref.model.ai.chatting.content;

public class TextContent extends MessageContent {
    public TextContent(String content) {
        super(content);
    }

    @Override
    public MessageContentType getType() {
        return MessageContentType.TEXT;
    }
}
