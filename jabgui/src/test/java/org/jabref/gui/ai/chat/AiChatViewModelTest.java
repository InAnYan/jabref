package org.jabref.gui.ai.chat;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.EndToEndJabGuiTest;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class AiChatViewModelTest {

    private AiPreferences aiPreferences;
    private ChatModel mockChatModel;
    private AnswerEngine mockAnswerEngine;
    private AiChatViewModel viewModel;

    private BibDatabaseContext databaseContext;
    private BibEntry entryWithPdf;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        aiPreferences = EndToEndJabGuiTest.createAiPreferences(true);

        mockChatModel = mock(ChatModel.class);
        mockAnswerEngine = mock(AnswerEngine.class);
        when(mockAnswerEngine.process(any(), any())).thenReturn(List.of());

        viewModel = new AiChatViewModel(
                aiPreferences,
                mock(FilePreferences.class),
                mock(DialogService.class),
                mock(IngestionTaskAggregator.class),
                mock(IngestedDocumentsRepository.class),
                mock(EmbeddingStore.class),
                mock(EmbeddingModelCache.class),
                new CurrentThreadTaskExecutor()
        );

        viewModel.chatModelProperty().set(mockChatModel);
        viewModel.answerEngineProperty().set(mockAnswerEngine);

        databaseContext = new BibDatabaseContext();
        entryWithPdf = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Entry2026")
                .withFiles(List.of(new LinkedFile("paper", "path/to/paper.pdf", "PDF")));
    }

    @Test
    void initialStateIsAiTurnedOffWhenAiDisabled() {
        aiPreferences.setAiFeaturesEnabledCurrently(false);

        assertEquals(AiChatViewModel.State.AI_TURNED_OFF, viewModel.stateProperty().get());
    }

    @Test
    void initialStateIsNoFilesWhenEntryHasNoAttachedPdf() {
        BibEntry entryWithoutFiles = new BibEntry(StandardEntryType.Article).withCitationKey("NoFiles2026");
        viewModel.entriesProperty().add(new FullBibEntry(databaseContext, entryWithoutFiles));

        assertEquals(AiChatViewModel.State.NO_FILES, viewModel.stateProperty().get());
    }

    @Test
    void initialStateIsIdleWhenEntryHasAttachedPdf() {
        viewModel.entriesProperty().add(new FullBibEntry(databaseContext, entryWithPdf));

        assertEquals(AiChatViewModel.State.IDLE, viewModel.stateProperty().get());
    }

    @Test
    void sendMessageAddsUserAndAiMessageWhenSuccessful() {
        viewModel.entriesProperty().add(new FullBibEntry(databaseContext, entryWithPdf));

        ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from("AI response summary")).build();
        when(mockChatModel.chat(any(List.class))).thenReturn(response);

        viewModel.sendMessage("What is this document about?");

        // Chat history contains: SYSTEM, USER, AI
        assertEquals(3, viewModel.chatHistoryProperty().size());
        assertEquals(ChatMessage.Role.SYSTEM, viewModel.chatHistoryProperty().getFirst().role());

        ChatMessage userMessage = viewModel.chatHistoryProperty().get(1);
        assertEquals(ChatMessage.Role.USER, userMessage.role());
        assertEquals("What is this document about?", userMessage.content());

        ChatMessage aiMessage = viewModel.chatHistoryProperty().get(2);
        assertEquals(ChatMessage.Role.AI, aiMessage.role());
        assertEquals("AI response summary", aiMessage.content());

        assertEquals(AiChatViewModel.State.IDLE, viewModel.stateProperty().get());
    }

    @Test
    void sendMessageFailureEntersErrorStateAndCancelRecovers() {
        viewModel.entriesProperty().add(new FullBibEntry(databaseContext, entryWithPdf));

        when(mockChatModel.chat(any(List.class))).thenThrow(new RuntimeException("Simulated API failure"));

        viewModel.sendMessage("Will fail");

        // History: SYSTEM, USER, ERROR
        assertEquals(3, viewModel.chatHistoryProperty().size());
        assertEquals(ChatMessage.Role.ERROR, viewModel.chatHistoryProperty().getLast().role());
        assertEquals(AiChatViewModel.State.ERROR, viewModel.stateProperty().get());

        viewModel.cancel();

        // Error removed -> SYSTEM, USER
        assertEquals(2, viewModel.chatHistoryProperty().size());
        assertEquals(ChatMessage.Role.USER, viewModel.chatHistoryProperty().getLast().role());
        assertEquals(AiChatViewModel.State.IDLE, viewModel.stateProperty().get());
    }

    @Test
    void regenerateAfterErrorRetriesLastUserMessage() {
        viewModel.entriesProperty().add(new FullBibEntry(databaseContext, entryWithPdf));

        when(mockChatModel.chat(any(List.class))).thenThrow(new RuntimeException("Simulated API failure"));
        viewModel.sendMessage("Retry question");
        assertEquals(AiChatViewModel.State.ERROR, viewModel.stateProperty().get());

        ChatResponse successResponse = ChatResponse.builder().aiMessage(AiMessage.from("Recovered answer")).build();
        when(mockChatModel.chat(any(List.class))).thenReturn(successResponse);

        viewModel.regenerate();

        // History: SYSTEM, USER, AI
        assertEquals(3, viewModel.chatHistoryProperty().size());
        assertEquals(ChatMessage.Role.USER, viewModel.chatHistoryProperty().get(1).role());
        assertEquals(ChatMessage.Role.AI, viewModel.chatHistoryProperty().get(2).role());
        assertEquals("Recovered answer", viewModel.chatHistoryProperty().get(2).content());
        assertEquals(AiChatViewModel.State.IDLE, viewModel.stateProperty().get());
    }

    @Test
    void deleteMessageRemovesSpecificMessageFromHistory() {
        viewModel.entriesProperty().add(new FullBibEntry(databaseContext, entryWithPdf));

        ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from("AI reply")).build();
        when(mockChatModel.chat(any(List.class))).thenReturn(response);

        viewModel.sendMessage("Question to delete");
        assertEquals(3, viewModel.chatHistoryProperty().size());

        ChatMessage userMessage = viewModel.chatHistoryProperty().get(1);
        viewModel.delete(userMessage.id());

        // History: SYSTEM, AI
        assertEquals(2, viewModel.chatHistoryProperty().size());
        assertFalse(viewModel.chatHistoryProperty().stream().anyMatch(m -> m.id().equals(userMessage.id())));
    }

    @Test
    void sendFollowUpMessageSendsQuestionDirectly() {
        viewModel.entriesProperty().add(new FullBibEntry(databaseContext, entryWithPdf));

        ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from("Follow up response")).build();
        when(mockChatModel.chat(any(List.class))).thenReturn(response);

        viewModel.sendFollowUpMessage("What are the key conclusions?");

        assertEquals(3, viewModel.chatHistoryProperty().size());
        assertEquals("What are the key conclusions?", viewModel.chatHistoryProperty().get(1).content());
        assertEquals("Follow up response", viewModel.chatHistoryProperty().get(2).content());
    }
}
