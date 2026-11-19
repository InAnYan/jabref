package org.jabref.gui.ai.chat;

import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class AiEntryChatViewModelTest {

    private AiPreferences aiPreferences;
    private InMemoryChatHistoryCache chatHistoryCache;
    private AiEntryChatViewModel viewModel;

    @BeforeEach
    void setUp() {
        aiPreferences = AiPreferences.getDefault();
        aiPreferences.setAiFeaturesEnabledCurrently(true);
        chatHistoryCache = mock(InMemoryChatHistoryCache.class);

        viewModel = new AiEntryChatViewModel(aiPreferences, chatHistoryCache);
    }

    @Test
    void enabledPropertyReflectsAiPreferences() {
        assertTrue(viewModel.enabledProperty().get());

        aiPreferences.setAiFeaturesEnabledCurrently(false);
        assertFalse(viewModel.enabledProperty().get());
    }

    @Test
    void selectingEntryLoadsEntryAndChatHistory() {
        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("EntryA");
        FullBibEntry fullEntry = new FullBibEntry(context, entry);

        List<ChatMessage> messages = List.of(
                ChatMessage.userMessage("Hello"),
                ChatMessage.aiMessage("Hi", List.of())
        );
        when(chatHistoryCache.getForEntry(context, entry)).thenReturn(FXCollections.observableArrayList(messages));

        viewModel.selectedEntryProperty().set(fullEntry);

        assertEquals(List.of(fullEntry), viewModel.entriesProperty().get());
        assertEquals(messages, viewModel.chatHistoryProperty().get());
    }

    @Test
    void switchingEntriesSwapsChatHistoryIndependently() {
        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry entryA = new BibEntry(StandardEntryType.Article).withCitationKey("EntryA");
        BibEntry entryB = new BibEntry(StandardEntryType.Article).withCitationKey("EntryB");

        FullBibEntry fullEntryA = new FullBibEntry(context, entryA);
        FullBibEntry fullEntryB = new FullBibEntry(context, entryB);

        List<ChatMessage> historyA = List.of(ChatMessage.userMessage("Question for A"));
        List<ChatMessage> historyB = List.of(ChatMessage.userMessage("Question for B"));

        when(chatHistoryCache.getForEntry(context, entryA)).thenReturn(FXCollections.observableArrayList(historyA));
        when(chatHistoryCache.getForEntry(context, entryB)).thenReturn(FXCollections.observableArrayList(historyB));

        viewModel.selectedEntryProperty().set(fullEntryA);
        assertEquals(historyA, viewModel.chatHistoryProperty().get());

        viewModel.selectedEntryProperty().set(fullEntryB);
        assertEquals(historyB, viewModel.chatHistoryProperty().get());
    }

    @Test
    void selectingEntryDoesNotLoadWhenAiDisabled() {
        aiPreferences.setAiFeaturesEnabledCurrently(false);

        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("EntryA");
        FullBibEntry fullEntry = new FullBibEntry(context, entry);

        viewModel.selectedEntryProperty().set(fullEntry);

        assertTrue(viewModel.entriesProperty().isEmpty());
        assertTrue(viewModel.chatHistoryProperty().isEmpty());
    }
}
