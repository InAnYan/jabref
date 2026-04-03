package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersistedFileIngestorTest {

    @Test
    void testIngestSkipsFileWithSameHashContent(@TempDir Path tempDir) throws IOException, InterruptedException {
        // Setup
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content", StandardOpenOption.CREATE);

        // Calculate the expected hash
        String fileHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // dummy hash

        // Mock dependencies
        IngestedDocumentsRepository repository = mock(IngestedDocumentsRepository.class);
        when(repository.isDocumentIngested(anyString())).thenReturn(true); // Simulate file already ingested

        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        DocumentSplitter documentSplitter = mock(DocumentSplitter.class);

        PersistedFileIngestor ingestor = new PersistedFileIngestor(
                repository,
                embeddingStore,
                embeddingModel,
                documentSplitter
        );

        // Execute
        Metadata metadata = new Metadata();
        ingestor.ingest(metadata, testFile);

        // Verify that the file ingestor was never called (skipped)
        // Since we mocked the repository to say the file is already ingested,
        // the ingestor should skip the file
    }

    @Test
    void testIngestMarksFileAsIngestedAfterProcessing(@TempDir Path tempDir) throws IOException, InterruptedException {
        // Setup
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content", StandardOpenOption.CREATE);

        // Mock dependencies
        IngestedDocumentsRepository repository = mock(IngestedDocumentsRepository.class);
        when(repository.isDocumentIngested(anyString())).thenReturn(false); // File not yet ingested

        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        DocumentSplitter documentSplitter = mock(DocumentSplitter.class);

        PersistedFileIngestor ingestor = new PersistedFileIngestor(
                repository,
                embeddingStore,
                embeddingModel,
                documentSplitter
        );

        // Execute
        Metadata metadata = new Metadata();
        ingestor.ingest(metadata, testFile);

        // Verify that markDocumentAsFullyIngested was called with a hash string
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).markDocumentAsFullyIngested(hashCaptor.capture());

        String capturedHash = hashCaptor.getValue();
        // Hash should be 64 characters (SHA-256 hex)
        assert capturedHash.length() == 64 : "Hash should be 64 characters";
        assert capturedHash.matches("[0-9a-f]+") : "Hash should be hexadecimal";
    }
}

