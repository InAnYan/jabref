package org.jabref.logic.ai.ingestion;

import java.nio.file.Path;

import org.jabref.logic.ai.pipeline.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.pipeline.repositories.MVStoreIngestedDocumentsRepository;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreIngestedDocumentsRepositoryTest extends IngestedDocumentsRepositoryTest {
    @Override
    IngestedDocumentsRepository makeTracker(Path path) {
        return new MVStoreIngestedDocumentsRepository(mock(NotificationService.class), path);
    }

    @Override
    void close(IngestedDocumentsRepository tracker) {
        ((MVStoreIngestedDocumentsRepository) tracker).close();
    }
}
