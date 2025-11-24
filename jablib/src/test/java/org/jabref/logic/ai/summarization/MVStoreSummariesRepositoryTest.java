package org.jabref.logic.ai.summarization;

import java.nio.file.Path;

import org.jabref.logic.ai.summarization.repositories.MVStoreSummariesRepository;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreSummariesRepositoryTest extends SummariesRepositoryTest {
    @Override
    SummariesRepository makeSummariesStorage(Path path) {
        return new MVStoreSummariesRepository(mock(NotificationService.class), path);
    }

    @Override
    void close(SummariesRepository summariesRepository) {
        ((MVStoreSummariesRepository) summariesRepository).close();
    }
}
