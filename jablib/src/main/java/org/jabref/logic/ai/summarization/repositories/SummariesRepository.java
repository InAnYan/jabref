package org.jabref.logic.ai.summarization.repositories;

import java.util.Optional;

import org.jabref.model.ai.identifiers.ResolvedBibEntryAiIdentifier;
import org.jabref.model.ai.summarization.BibEntrySummary;

public interface SummariesRepository {
    void set(ResolvedBibEntryAiIdentifier identifier, BibEntrySummary bibEntrySummary);

    Optional<BibEntrySummary> get(ResolvedBibEntryAiIdentifier identifier);

    void clear(ResolvedBibEntryAiIdentifier identifier);
}
