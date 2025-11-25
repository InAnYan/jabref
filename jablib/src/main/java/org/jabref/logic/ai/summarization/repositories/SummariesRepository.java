package org.jabref.logic.ai.summarization.repositories;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.ai.summarization.BibEntrySummary;

public interface SummariesRepository {
    void set(Path bibDatabasePath, String citationKey, BibEntrySummary bibEntrySummary);

    Optional<BibEntrySummary> get(Path bibDatabasePath, String citationKey);

    void clear(Path bibDatabasePath, String citationKey);
}
