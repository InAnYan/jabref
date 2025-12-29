package org.jabref.gui.ai.chat;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.groups.GroupNodeViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.util.ChatHistoryFactory;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.GroupChatHistoryIdentifier;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.database.BibDatabaseContext;

public class AiGroupChatViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_DATABASE_PATH,
        CHATTING
    }

    private final AiPreferences aiPreferences;
    private final ChatHistoryRepository chatHistoryRepository;

    private final ObjectProperty<GroupNodeViewModel> groupNode = new SimpleObjectProperty<>();
    private final ObjectProperty<BibDatabaseContext> databaseContext = new SimpleObjectProperty<>();

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.NO_DATABASE_PATH);
    private final ListProperty<BibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ChatHistoryRecordV2> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty windowTitle = new SimpleStringProperty();

    public AiGroupChatViewModel(GuiPreferences preferences, AiService aiService) {
        this.aiPreferences = preferences.getAiPreferences();
        this.chatHistoryRepository = aiService.getChattingFeature().getChatHistoryRepository();

        aiPreferences.enableAiProperty().addListener((_, _, value) -> {
            if (!value) {
                state.set(State.AI_TURNED_OFF);
            } else {
                refreshState();
            }
        });

        this.groupNode.addListener(_ -> refreshState());
        this.databaseContext.addListener(_ -> refreshState());
    }

    private void refreshState() {
        if (!aiPreferences.getEnableAi()) {
            state.set(State.AI_TURNED_OFF);
            return;
        }

        if (databaseContext.get() == null || groupNode.get() == null) {
            return;
        }

        BibDatabaseContext context = databaseContext.get();
        GroupNodeViewModel group = groupNode.get();

        if (context.getDatabasePath().isEmpty()) {
            state.set(State.NO_DATABASE_PATH);
        } else {
            loadGroupChat(context, group);
            updateTitle(context, group);
            state.set(State.CHATTING);
        }
    }

    private void loadGroupChat(BibDatabaseContext context, GroupNodeViewModel group) {
        assert context.getDatabasePath().isPresent();

        List<BibEntryAiIdentifier> matchedEntries = group
                .getGroupNode()
                .findMatches(context.getDatabase())
                .stream()
                .map(entry -> new BibEntryAiIdentifier(context, entry))
                .toList();

        entries.set(FXCollections.observableArrayList(matchedEntries));

        chatHistory.set(ChatHistoryFactory.makeChatHistoryProperty(
                new GroupChatHistoryIdentifier(
                        context.getDatabasePath().get(),
                        group.getGroupNode().getGroup().nameProperty().get()
                ),
                chatHistoryRepository
        ));
    }

    private void updateTitle(BibDatabaseContext context, GroupNodeViewModel group) {
        String groupName = group.getGroupNode().getGroup().getName();
        String libraryName = context.getDatabasePath()
                                    .map(Path::getFileName)
                                    .map(Path::toString)
                                    .orElse(Localization.lang("Untitled"));

        windowTitle.set("%s — %s".formatted(groupName, libraryName));
    }

    public ObjectProperty<GroupNodeViewModel> groupNodeProperty() {
        return groupNode;
    }

    public ObjectProperty<BibDatabaseContext> databaseContextProperty() {
        return databaseContext;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ListProperty<BibEntryAiIdentifier> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatHistoryRecordV2> chatHistoryProperty() {
        return chatHistory;
    }

    public StringProperty windowTitleProperty() {
        return windowTitle;
    }
}
