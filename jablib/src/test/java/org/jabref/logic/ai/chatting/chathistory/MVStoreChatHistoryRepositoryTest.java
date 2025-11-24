package org.jabref.logic.ai.chatting.chathistory;

import java.nio.file.Path;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.MVStoreChatHistoryRepository;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreChatHistoryRepositoryTest extends ChatHistoryRepositoryTest {
    @Override
    ChatHistoryRepository makeStorage(Path path) {
        return new MVStoreChatHistoryRepository(mock(NotificationService.class), path);
    }

    @Override
    void close(ChatHistoryRepository storage) {
        ((MVStoreChatHistoryRepository) storage).close();
    }
}
