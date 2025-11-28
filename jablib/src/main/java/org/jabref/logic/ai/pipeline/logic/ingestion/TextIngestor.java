package org.jabref.logic.ai.pipeline.logic.ingestion;

import java.util.List;

import org.jabref.logic.ai.pipeline.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.util.LongTaskInfo;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

public class TextIngestor {
    private final EmbeddingStoreIngestor ingestor;
    private final DocumentSplitter documentSplitter;

    public TextIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter
    ) {
        this.ingestor = EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                // TODO: remove this stub.
                .documentSplitter(document -> List.of(new TextSegment(document.text(), document.metadata())))
                .build();

        this.documentSplitter = documentSplitter;
    }

    public void ingest(
            LongTaskInfo longTaskInfo,
            Metadata metadata,
            String text
    ) throws InterruptedException {
        List<String> chunks = documentSplitter.split(longTaskInfo, text).toList();
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
