package org.jabref.logic.ai.summarization.repositories;

import java.util.Optional;

import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.summarization.BibEntrySummary;

public interface SummariesRepository {
    void set(BibEntryAiIdentifier identifier, BibEntrySummary bibEntrySummary);

    Optional<BibEntrySummary> get(BibEntryAiIdentifier identifier);

    void clear(BibEntryAiIdentifier identifier);
}
