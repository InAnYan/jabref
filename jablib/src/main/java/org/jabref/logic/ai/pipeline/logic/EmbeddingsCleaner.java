package org.jabref.logic.ai.pipeline.logic;

import java.util.List;

import org.jabref.logic.ai.pipeline.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
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

    public void clearEmbeddingsFor(List<LinkedFile> linkedFiles) {
        linkedFiles.stream().map(LinkedFile::getLink).forEach(this::removeDocument);
    }
}
