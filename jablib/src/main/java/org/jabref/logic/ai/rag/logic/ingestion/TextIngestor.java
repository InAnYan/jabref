package org.jabref.logic.ai.rag.logic.ingestion;

import java.util.List;

import org.jabref.logic.ai.rag.logic.documentsplitting.DocumentSplitterAlgorithm;
import org.jabref.logic.ai.util.LongTaskInfo;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

public class TextIngestor {
    private final EmbeddingStoreIngestor ingestor;
    private final DocumentSplitterAlgorithm documentSplitterAlgorithm;

    public TextIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitterAlgorithm documentSplitterAlgorithm
    ) {
        this.ingestor = EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

        this.documentSplitterAlgorithm = documentSplitterAlgorithm;
    }

    public void ingest(
            LongTaskInfo longTaskInfo,
            Metadata metadata,
            String text
    ) throws InterruptedException {
        List<String> chunks = documentSplitterAlgorithm.split(longTaskInfo, text).toList();
        longTaskInfo.progressCounter().increaseWorkMax(chunks.size());

        for (String documentPart : chunks) {
            if (longTaskInfo.shutdownSignal().get()) {
                throw new InterruptedException();
            }

            ingestor.ingest(new DefaultDocument(documentPart, metadata));

            longTaskInfo.progressCounter().increaseWorkDone(1);
        }
    }
}
