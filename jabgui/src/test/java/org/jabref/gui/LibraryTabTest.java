package org.jabref.gui;

import java.util.UUID;

import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryTabTest {

    @Test
    void ensureAiLibraryIdPresentGeneratesIdWhenMissing() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();

        LibraryTab.ensureAiLibraryIdPresent(databaseContext);

        assertTrue(databaseContext.getMetaData().getAiLibraryId().isPresent());
        assertDoesNotThrow(() -> UUID.fromString(databaseContext.getMetaData().getAiLibraryId().orElseThrow()));
    }

    @Test
    void ensureAiLibraryIdPresentKeepsExistingId() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.getMetaData().setAiLibraryId("existing-ai-library-id");

        LibraryTab.ensureAiLibraryIdPresent(databaseContext);

        assertEquals("existing-ai-library-id", databaseContext.getMetaData().getAiLibraryId().orElseThrow());
    }
}


