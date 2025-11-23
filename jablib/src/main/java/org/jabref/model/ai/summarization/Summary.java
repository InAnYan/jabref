package org.jabref.model.ai.summarization;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.jabref.model.ai.chatting.AiProvider;

public record Summary(LocalDateTime timestamp, AiProvider aiProvider, String model, String content) implements Serializable {
}
