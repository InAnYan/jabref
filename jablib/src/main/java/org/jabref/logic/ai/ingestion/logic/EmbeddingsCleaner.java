package org.jabref.logic.ai.ingestion.logic;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;

public class EmbeddingsCleaner {
    public static final String LINK_METADATA_KEY = "link";

    private final AiPreferences aiPreferences;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;

    public EmbeddingsCleaner(
            AiPreferences aiPreferences,
            EmbeddingStore<TextSegment> embeddingStore,
            IngestedDocumentsRepository ingestedDocumentsRepository
    ) {
        this.aiPreferences = aiPreferences;
        this.embeddingStore = embeddingStore;
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;

        setupListeningToPreferencesChanges();
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.addListenerToEmbeddingsParametersChange(embeddingStore::removeAll);
    }

    public void removeDocument(String link) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey(LINK_METADATA_KEY).isEqualTo(link));
        ingestedDocumentsRepository.unmarkDocumentAsFullyIngested(link);
    }

    public void clearEmbeddingsFor(List<LinkedFile> linkedFiles, BibDatabaseContext bibDatabaseContext, FilePreferences filePreferences) {
        linkedFiles.stream()
                   .flatMap(linkedFile -> linkedFile.findIn(bibDatabaseContext, filePreferences).stream())
                   .map(Path::toString)
                   .forEach(this::removeDocument);
    }
}
