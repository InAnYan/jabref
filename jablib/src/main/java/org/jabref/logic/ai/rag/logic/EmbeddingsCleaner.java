package org.jabref.logic.ai.rag.logic;

import java.util.List;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.repositories.FullyIngestedDocumentsRepository;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;

public class EmbeddingsCleaner {
    public static final String LINK_METADATA_KEY = "link";

    private final AiPreferences aiPreferences;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final FullyIngestedDocumentsRepository fullyIngestedDocumentsRepository;

    public EmbeddingsCleaner(
            AiPreferences aiPreferences,
            EmbeddingStore<TextSegment> embeddingStore,
            FullyIngestedDocumentsRepository fullyIngestedDocumentsRepository
    ) {
        this.aiPreferences = aiPreferences;
        this.embeddingStore = embeddingStore;
        this.fullyIngestedDocumentsRepository = fullyIngestedDocumentsRepository;

        setupListeningToPreferencesChanges();
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.addListenerToEmbeddingsParametersChange(embeddingStore::removeAll);
    }

    public void removeDocument(String link) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey(LINK_METADATA_KEY).isEqualTo(link));
        fullyIngestedDocumentsRepository.unmarkDocumentAsFullyIngested(link);
    }

    public void clearEmbeddingsFor(List<LinkedFile> linkedFiles) {
        linkedFiles.stream().map(LinkedFile::getLink).forEach(this::removeDocument);
    }
}
