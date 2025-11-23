package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.ai.Summary;

public interface SummariesStorage {
    void set(Path bibDatabasePath, String citationKey, Summary summary);

    Optional<Summary> get(Path bibDatabasePath, String citationKey);

    void clear(Path bibDatabasePath, String citationKey);
}
