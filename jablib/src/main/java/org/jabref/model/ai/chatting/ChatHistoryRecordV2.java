package org.jabref.model.ai.chatting;

import java.io.Serializable;
import java.time.Instant;

public record ChatHistoryRecordV2(
        String id,
        String messageTypeClassName,
        String content,
        Instant createdAt
) implements Serializable {
}
