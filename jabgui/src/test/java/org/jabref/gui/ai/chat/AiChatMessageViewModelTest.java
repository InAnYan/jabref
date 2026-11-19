package org.jabref.gui.ai.chat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.scene.input.ClipboardContent;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.model.ai.chatting.ChatMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(ApplicationExtension.class)
class AiChatMessageViewModelTest {

    private ClipBoardManager clipBoardManager;
    private AiChatMessageViewModel viewModel;

    @BeforeEach
    void setUp() {
        clipBoardManager = mock(ClipBoardManager.class);
        viewModel = new AiChatMessageViewModel(clipBoardManager);
    }

    @Test
    void propertiesUpdateWhenChatMessageIsSet() {
        ChatMessage message = ChatMessage.userMessage("Test message");
        viewModel.chatMessageProperty().set(message);

        assertEquals(message.id(), viewModel.idProperty().get());
        assertEquals("Test message", viewModel.messageContentProperty().get());
        assertEquals(message.timestamp(), viewModel.timestampProperty().get());
        assertFalse(viewModel.showRegenerateProperty().get());
    }

    @Test
    void showRegenerateIsTrueForAiAndErrorRoles() {
        ChatMessage aiMessage = ChatMessage.aiMessage("AI answer", List.of());
        viewModel.chatMessageProperty().set(aiMessage);
        assertTrue(viewModel.showRegenerateProperty().get());

        ChatMessage errorMessage = ChatMessage.errorMessage(new RuntimeException("Failure"));
        viewModel.chatMessageProperty().set(errorMessage);
        assertTrue(viewModel.showRegenerateProperty().get());

        ChatMessage userMessage = ChatMessage.userMessage("User text");
        viewModel.chatMessageProperty().set(userMessage);
        assertFalse(viewModel.showRegenerateProperty().get());
    }

    @Test
    void copyToClipboardSetsMessageContentOnClipBoardManager() {
        ChatMessage message = ChatMessage.userMessage("Text to copy");
        viewModel.chatMessageProperty().set(message);

        viewModel.copyToClipboard();

        ArgumentCaptor<ClipboardContent> captor = ArgumentCaptor.forClass(ClipboardContent.class);
        verify(clipBoardManager).setContent(captor.capture());
        assertEquals("Text to copy", captor.getValue().getString());
    }

    @Test
    void deleteTriggersOnDeleteHandler() {
        AtomicBoolean deleteCalled = new AtomicBoolean(false);
        viewModel.onDeleteProperty().set(_ -> deleteCalled.set(true));

        viewModel.delete();

        assertTrue(deleteCalled.get());
    }

    @Test
    void regenerateTriggersOnRegenerateHandler() {
        AtomicBoolean regenerateCalled = new AtomicBoolean(false);
        viewModel.onRegenerateProperty().set(_ -> regenerateCalled.set(true));

        viewModel.regenerate();

        assertTrue(regenerateCalled.get());
    }
}
