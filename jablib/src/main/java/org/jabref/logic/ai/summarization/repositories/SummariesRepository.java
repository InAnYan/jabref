package org.jabref.logic.ai.summarization.repositories;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.ai.summarization.Summary;

public interface SummariesRepository {
    void set(Path bibDatabasePath, String citationKey, Summary summary);

    Optional<Summary> get(Path bibDatabasePath, String citationKey);

    void clear(Path bibDatabasePath, String citationKey);
}
