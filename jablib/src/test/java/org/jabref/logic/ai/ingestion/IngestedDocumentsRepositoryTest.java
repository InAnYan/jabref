package org.jabref.logic.ai.ingestion;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.ai.pipeline.repositories.IngestedDocumentsRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class IngestedDocumentsRepositoryTest {
    @TempDir Path tempDir;

    private IngestedDocumentsRepository tracker;

    abstract IngestedDocumentsRepository makeTracker(Path path);

    abstract void close(IngestedDocumentsRepository tracker);

    @BeforeEach
    void setUp() {
        tracker = makeTracker(tempDir.resolve("test.bib"));
    }

    private void reopen() {
        close(tracker);
        setUp();
    }

    @AfterEach
    void tearDown() {
        close(tracker);
    }

    @Test
    void markDocumentAsFullyIngested() {
        tracker.markDocumentAsFullyIngested("link", 1L);
        reopen();
        assertEquals(Optional.of(1L), tracker.getIngestedDocumentModificationTimeInSeconds("link"));
    }

    @Test
    void unmarkDocumentAsFullyIngested() {
        tracker.markDocumentAsFullyIngested("link", 1L);
        reopen();
        tracker.unmarkDocumentAsFullyIngested("link");
        reopen();
        assertEquals(Optional.empty(), tracker.getIngestedDocumentModificationTimeInSeconds("link"));
    }
}
