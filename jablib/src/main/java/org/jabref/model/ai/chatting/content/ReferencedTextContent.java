package org.jabref.model.ai.chatting.content;

import java.util.List;

public class ReferencedTextContent extends MessageContent {
    private final List<String> references;

    public ReferencedTextContent(String content, List<String> references) {
        super(content);

        this.references = references;
    }

    public List<String> getReferences() {
        return references;
    }

    @Override
    public MessageContentType getType() {
        return MessageContentType.REFERENCED_TEXT;
    }
}
