package org.jabref.logic.ai.chatting;

import java.util.Optional;

import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabaseContext;

import dev.langchain4j.data.message.ChatMessage;

// Note about `Optional<BibDatabaseContext>`: it was necessary in a previous version, but currently we never save an `Optional.empty()`.
// However, we decided to leave it here: to reduce migrations and to make it possible to chat with a {@link BibEntry} without {@link BibDatabaseContext}
// ({@link BibDatabaseContext} is required only for load/store of the chat).
public record ChatHistoryManagementRecord(
        Optional<BibDatabaseContext> bibDatabaseContext,
        ObservableList<ChatMessage> chatHistory
) {
}
