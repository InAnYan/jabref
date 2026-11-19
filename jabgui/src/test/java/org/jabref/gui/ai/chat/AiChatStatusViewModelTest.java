package org.jabref.gui.ai.chat;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;

import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class AiChatStatusViewModelTest {

    private DialogService dialogService;
    private AiChatStatusViewModel viewModel;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        dialogService = mock(DialogService.class);

        viewModel = new AiChatStatusViewModel(
                AiPreferences.getDefault(),
                mock(FilePreferences.class),
                mock(FieldPreferences.class),
                new BibEntryTypesManager(),
                dialogService,
                mock(EmbeddingModelCache.class),
                mock(EmbeddingStore.class)
        );
    }

    @Test
    void clearChatHistoryClearsMessagesWhenConfirmed() {
        viewModel.chatHistoryProperty().add(ChatMessage.userMessage("Question"));
        viewModel.chatHistoryProperty().add(ChatMessage.aiMessage("Answer", List.of()));
        assertFalse(viewModel.chatHistoryProperty().isEmpty());

        when(dialogService.showConfirmationDialogAndWait(any(), any())).thenReturn(true);

        viewModel.clearChatHistory();

        assertTrue(viewModel.chatHistoryProperty().isEmpty());
    }

    @Test
    void clearChatHistoryKeepsMessagesWhenRejected() {
        viewModel.chatHistoryProperty().add(ChatMessage.userMessage("Question"));
        viewModel.chatHistoryProperty().add(ChatMessage.aiMessage("Answer", List.of()));
        assertEquals(2, viewModel.chatHistoryProperty().size());

        when(dialogService.showConfirmationDialogAndWait(any(), any())).thenReturn(false);

        viewModel.clearChatHistory();

        assertEquals(2, viewModel.chatHistoryProperty().size());
    }

    @Test
    void entriesPropertyReflectsAttachedBibEntriesAndFiles() {
        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Test2026")
                .withFiles(List.of(new LinkedFile("paper", "path/to/paper.pdf", "PDF")));

        FullBibEntry fullBibEntry = new FullBibEntry(context, entry);
        viewModel.entriesProperty().add(fullBibEntry);

        assertEquals(1, viewModel.entriesProperty().size());
        assertEquals(1, viewModel.entriesProperty().getFirst().entry().getFiles().size());
    }

    @Test
    void setAnswerEngineUpdatesSelectedAnswerEngineKind() {
        AnswerEngine answerEngine = mock(AnswerEngine.class);
        when(answerEngine.getKind()).thenReturn(AnswerEngineKind.FULL_DOCUMENT);

        viewModel.setAnswerEngine(answerEngine);

        assertEquals(AnswerEngineKind.FULL_DOCUMENT, viewModel.selectedAnswerEngineKindProperty().get());
    }
}
