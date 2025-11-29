package org.jabref.logic.ai.chatting.chathistory;

import java.nio.file.Path;

import org.jabref.logic.ai.chatting.repositories.MVStoreChatHistoryRepositoryV1;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreChatHistoryRepositoryV1Test extends EntryChatHistoryRepositoryV1Test {
    @Override
    ChatHistoryRepository makeStorage(Path path) {
        return new MVStoreChatHistoryRepositoryV1(mock(NotificationService.class), path);
    }

    @Override
    void close(ChatHistoryRepository storage) {
        ((MVStoreChatHistoryRepositoryV1) storage).close();
    }
}
