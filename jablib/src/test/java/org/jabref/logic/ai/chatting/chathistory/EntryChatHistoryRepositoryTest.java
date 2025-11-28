package org.jabref.logic.ai.chatting.chathistory;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class EntryChatHistoryRepositoryTest {
    @TempDir Path tempDir;

    private EntryChatHistoryRepository storage;

    abstract EntryChatHistoryRepository makeStorage(Path path);

    abstract void close(EntryChatHistoryRepository storage);

    @BeforeEach
    void setUp() {
        storage = makeStorage(tempDir.resolve("test.bib"));
    }

    private void reopen() {
        close(storage);
        setUp();
    }

    @AfterEach
    void tearDown() {
        close(storage);
    }

    @Test
    void entryChatHistory() {
        List<ChatMessage> messages = List.of(
                new UserMessage("hi!"),
                new AiMessage("hello!")
        );

        BibEntryAiIdentifier identifier = new BibEntryAiIdentifier(
                tempDir.resolve("test.bib"),
                "citationKey"
        );
        storage.storeMessagesForEntry(identifier, messages);

        reopen();

        assertEquals(messages, storage.loadMessagesForEntry(identifier));
    }

    @Test
    void groupChatHistory() {
        List<ChatMessage> messages = List.of(
                new UserMessage("hi!"),
                new AiMessage("hello!")
        );

        GroupAiIdentifier identifier = new GroupAiIdentifier(tempDir.resolve("test.bib"), "group");
        storage.storeMessagesForGroup(identifier, messages);

        reopen();

        assertEquals(messages, storage.loadMessagesForGroup(identifier));
    }
}
