package org.jabref.logic.ai.chatting.chathistory;

import java.nio.file.Path;

import org.jabref.logic.ai.chatting.storages.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.storages.MVStoreChatHistoryRepository;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreChatHistoryRepositoryTest extends ChatHistoryRepositoryTest {
    @Override
    ChatHistoryRepository makeStorage(Path path) {
        return new MVStoreChatHistoryRepository(path, mock(NotificationService.class));
    }

    @Override
    void close(ChatHistoryRepository storage) {
        ((MVStoreChatHistoryRepository) storage).close();
    }
}
