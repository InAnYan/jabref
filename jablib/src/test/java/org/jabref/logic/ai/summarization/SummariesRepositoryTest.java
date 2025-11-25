package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.model.ai.chatting.AiProvider;
import org.jabref.model.ai.summarization.BibEntrySummary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class SummariesRepositoryTest {
    @TempDir Path tempDir;

    private SummariesRepository summariesRepository;
    private Path bibPath;

    abstract SummariesRepository makeSummariesStorage(Path path);

    abstract void close(SummariesRepository summariesRepository);

    @BeforeEach
    void setUp() {
        bibPath = tempDir.resolve("test.bib");
        summariesRepository = makeSummariesStorage(tempDir.resolve("test.bib"));
    }

    private void reopen() {
        close(summariesRepository);
        setUp();
    }

    @AfterEach
    void tearDown() {
        close(summariesRepository);
    }

    @Test
    void set() {
        summariesRepository.set(bibPath, "citationKey", new BibEntrySummary(LocalDateTime.now(), AiProvider.OPEN_AI, "model", "contents"));
        reopen();
        assertEquals(Optional.of("contents"), summariesRepository.get(bibPath, "citationKey").map(BibEntrySummary::content));
    }

    @Test
    void clear() {
        summariesRepository.set(bibPath, "citationKey", new BibEntrySummary(LocalDateTime.now(), AiProvider.OPEN_AI, "model", "contents"));
        reopen();
        summariesRepository.clear(bibPath, "citationKey");
        reopen();
        assertEquals(Optional.empty(), summariesRepository.get(bibPath, "citationKey"));
    }
}
