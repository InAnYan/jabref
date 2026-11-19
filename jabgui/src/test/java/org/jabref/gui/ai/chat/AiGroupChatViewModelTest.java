package org.jabref.gui.ai.chat;

import java.util.List;
import java.util.UUID;

import javafx.collections.FXCollections;

import org.jabref.gui.groups.GroupNodeViewModel;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

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
class AiGroupChatViewModelTest {

    private AiPreferences aiPreferences;
    private InMemoryChatHistoryCache chatHistoryCache;
    private AiGroupChatViewModel viewModel;

    @BeforeEach
    void setUp() {
        aiPreferences = AiPreferences.getDefault();
        aiPreferences.setAiFeaturesEnabledCurrently(true);
        chatHistoryCache = mock(InMemoryChatHistoryCache.class);
        AiService aiService = mock(AiService.class);
        when(aiService.getChatHistoryCache()).thenReturn(chatHistoryCache);

        viewModel = new AiGroupChatViewModel(aiPreferences, aiService);
    }

    @Test
    void enabledPropertyReflectsAiPreferences() {
        assertTrue(viewModel.enabledProperty().get());

        aiPreferences.setAiFeaturesEnabledCurrently(false);
        assertFalse(viewModel.enabledProperty().get());
    }

    @Test
    void settingGroupNodeAndDatabaseLoadsMatchedEntriesAndChatHistory() {
        BibDatabaseContext context = new BibDatabaseContext();
        context.getMetaData().setAiLibraryId(UUID.randomUUID().toString());

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("GroupArticle2026")
                .withField(StandardField.GROUPS, "MachineLearning");
        context.getDatabase().insertEntry(entry);

        ExplicitGroup group = new ExplicitGroup("MachineLearning", GroupHierarchyType.INDEPENDENT, ',');
        GroupTreeNode groupTreeNode = GroupTreeNode.fromGroup(group);

        GroupNodeViewModel groupNodeVM = mock(GroupNodeViewModel.class);
        when(groupNodeVM.getGroupNode()).thenReturn(groupTreeNode);

        List<ChatMessage> messages = List.of(ChatMessage.userMessage("Discuss ML"));
        when(chatHistoryCache.getForGroup(context, groupTreeNode)).thenReturn(FXCollections.observableArrayList(messages));

        viewModel.databaseContextProperty().set(context);
        viewModel.groupNodeProperty().set(groupNodeVM);

        assertEquals(1, viewModel.entriesProperty().size());
        assertEquals(entry, viewModel.entriesProperty().getFirst().entry());
        assertEquals(messages, viewModel.chatHistoryProperty().get());
    }

    @Test
    void switchingGroupsSwapsChatHistoryIndependently() {
        BibDatabaseContext context = new BibDatabaseContext();
        context.getMetaData().setAiLibraryId(UUID.randomUUID().toString());

        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Entry1")
                .withField(StandardField.GROUPS, "GroupA");
        BibEntry entry2 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Entry2")
                .withField(StandardField.GROUPS, "GroupB");
        context.getDatabase().insertEntry(entry1);
        context.getDatabase().insertEntry(entry2);

        GroupTreeNode groupTreeNodeA = GroupTreeNode.fromGroup(new ExplicitGroup("GroupA", GroupHierarchyType.INDEPENDENT, ','));
        GroupTreeNode groupTreeNodeB = GroupTreeNode.fromGroup(new ExplicitGroup("GroupB", GroupHierarchyType.INDEPENDENT, ','));

        GroupNodeViewModel groupVMA = mock(GroupNodeViewModel.class);
        when(groupVMA.getGroupNode()).thenReturn(groupTreeNodeA);

        GroupNodeViewModel groupVMB = mock(GroupNodeViewModel.class);
        when(groupVMB.getGroupNode()).thenReturn(groupTreeNodeB);

        List<ChatMessage> historyA = List.of(ChatMessage.userMessage("Topic A"));
        List<ChatMessage> historyB = List.of(ChatMessage.userMessage("Topic B"));

        when(chatHistoryCache.getForGroup(context, groupTreeNodeA)).thenReturn(FXCollections.observableArrayList(historyA));
        when(chatHistoryCache.getForGroup(context, groupTreeNodeB)).thenReturn(FXCollections.observableArrayList(historyB));

        viewModel.databaseContextProperty().set(context);
        viewModel.groupNodeProperty().set(groupVMA);
        assertEquals(historyA, viewModel.chatHistoryProperty().get());

        viewModel.groupNodeProperty().set(groupVMB);
        assertEquals(historyB, viewModel.chatHistoryProperty().get());
    }
}
