package org.jabref.model.ai;

import java.io.Serializable;
import java.time.LocalDateTime;

public record Summary(LocalDateTime timestamp, AiProvider aiProvider, String model, String content) implements Serializable {
}
