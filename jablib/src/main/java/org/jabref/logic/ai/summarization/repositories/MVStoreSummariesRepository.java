package org.jabref.logic.ai.summarization.repositories;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.summarization.BibEntrySummary;

public class MVStoreSummariesRepository extends MVStoreBase implements SummariesRepository {
    private static final String SUMMARIES_MAP_PREFIX = "summaries";

    public MVStoreSummariesRepository(NotificationService dialogService, Path path) {
        super(path, dialogService);
    }

    public void set(BibEntryAiIdentifier identifier, BibEntrySummary bibEntrySummary) {
        getMap(identifier.databasePath()).put(identifier.citationKey(), bibEntrySummary);
    }

    public Optional<BibEntrySummary> get(BibEntryAiIdentifier identifier) {
        return Optional.ofNullable(getMap(identifier.databasePath()).get(identifier.citationKey()));
    }

    public void clear(BibEntryAiIdentifier identifier) {
        getMap(identifier.databasePath()).remove(identifier.citationKey());
    }

    private Map<String, BibEntrySummary> getMap(Path bibDatabasePath) {
        return mvStore.openMap(SUMMARIES_MAP_PREFIX + "-" + bibDatabasePath.toString());
    }

    @Override
    protected String errorMessageForOpening() {
        return "An error occurred while opening summary storage. Summaries of entries will not be stored in the next session.";
    }

    @Override
    protected String errorMessageForOpeningLocalized() {
        return Localization.lang("An error occurred while opening summary storage. Summaries of entries will not be stored in the next session.");
    }
}
