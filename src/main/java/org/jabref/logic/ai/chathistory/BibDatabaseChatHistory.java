package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;


/**
 * This class stores the chat history with AI. The chat history file is stored alongside BIB database.
 * <p>
 * It uses MVStore for serializing the messages. In case any error occurs while opening the MVStore,
 * the class will notify the user of this error and continue with in-memory store (meaning all messages will
 * be thrown away on exit).
 */
public class BibDatabaseChatHistory {
    public static final String AI_CHATS_FILE_EXTENSION = "aichats";

    private final MVStore mvStore;

    private final MVMap<Integer, ChatMessage.Type> messageType;
    private final MVMap<Integer, String> messageContent;
    private final MVMap<Integer, String> messageCitationKey;

    public BibDatabaseChatHistory(Path bibDatabasePath, DialogService dialogService) {
        MVStore mvStore1; // This Java again...

        try {
            mvStore1 = MVStore.open(bibDatabasePath + "." + AI_CHATS_FILE_EXTENSION);
        } catch (Exception e) {
            dialogService.showErrorDialogAndWait("Unable to open chat history store for the library. Will use an in-memory store", e);
            mvStore1 = MVStore.open(null);
        }

        this.mvStore = mvStore1;

        this.messageType = this.mvStore.openMap("messageType");
        this.messageContent = this.mvStore.openMap("messageContent");
        this.messageCitationKey = this.mvStore.openMap("messageCitationKey");
    }

    public BibEntryChatHistory getChatHistoryForEntry(String citationKey) {
        return new BibEntryChatHistory(this, citationKey);
    }

    public Stream<ChatMessage> getAllMessagesForEntry(String citationKey) {
        return messageCitationKey
                .entrySet()
                .stream()
                .filter(integerStringEntry -> integerStringEntry.getValue().equals(citationKey))
                .map(Map.Entry::getKey)
                .sorted() // Violating normal forms :)
                .map(id -> new ChatMessage(messageType.get(id), messageContent.get(id)));
    }

    public void addMessage(String citationKey, ChatMessage message) {
        int id = getMaxInt() + 1;

        this.messageType.put(id, message.getType());
        this.messageContent.put(id, message.getContent());
        this.messageCitationKey.put(id, citationKey);
    }

    public void clearMessagesForEntry(String citationKey) {
        messageCitationKey
                .entrySet()
                .stream()
                .filter(integerStringEntry -> integerStringEntry.getValue().equals(citationKey))
                .map(Map.Entry::getKey)
                .forEach(id -> {
                    messageType.remove(id);
                    messageContent.remove(id);
                    messageCitationKey.remove(id);
                });
    }

    public void close() {
        this.mvStore.close();
    }

    private int getMaxInt() {
        synchronized (messageContent) {
            return messageContent.size();
        }
    }
}

