package org.jabref.logic.ai.ingestion;

import java.nio.file.Path;

import org.jabref.logic.ai.rag.repositories.FullyIngestedDocumentsRepository;
import org.jabref.logic.ai.rag.repositories.MVStoreFullyIngestedDocumentsRepository;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreFullyIngestedDocumentsRepositoryTest extends FullyIngestedDocumentsRepositoryTest {
    @Override
    FullyIngestedDocumentsRepository makeTracker(Path path) {
        return new MVStoreFullyIngestedDocumentsRepository(path, mock(NotificationService.class));
    }

    @Override
    void close(FullyIngestedDocumentsRepository tracker) {
        ((MVStoreFullyIngestedDocumentsRepository) tracker).close();
    }
}
