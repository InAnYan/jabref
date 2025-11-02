package org.jabref.logic.ai.chatting.chathistory;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.ai.framework.messages.ChatMessage;

public interface ChatHistoryStorage {
    List<ChatMessage> loadMessagesForEntry(Path bibDatabasePath, String citationKey);

    void storeMessagesForEntry(Path bibDatabasePath, String citationKey, List<ChatMessage> messages);

    List<ChatMessage> loadMessagesForGroup(Path bibDatabasePath, String name);

    void storeMessagesForGroup(Path bibDatabasePath, String name, List<ChatMessage> messages);

    void commit();

    void close();
}
