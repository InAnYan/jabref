package org.jabref.logic.ai.summarization;

import java.nio.file.Path;

import org.jabref.logic.ai.summarization.storages.MVStoreSummariesRepository;
import org.jabref.logic.ai.summarization.storages.SummariesRepository;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreSummariesRepositoryTest extends SummariesRepositoryTest {
    @Override
    SummariesRepository makeSummariesStorage(Path path) {
        return new MVStoreSummariesRepository(path, mock(NotificationService.class));
    }

    @Override
    void close(SummariesRepository summariesRepository) {
        ((MVStoreSummariesRepository) summariesRepository).close();
    }
}
