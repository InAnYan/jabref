package org.jabref.gui.ai.chat;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.groups.GroupNodeViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class AiGroupChatViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_DATABASE_PATH,
        CHATTING
    }

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.NO_DATABASE_PATH);

    private final ObjectProperty<GroupNodeViewModel> groupNode = new SimpleObjectProperty<>();
    private final ObjectProperty<BibDatabaseContext> databaseContext = new SimpleObjectProperty<>();

    private final ListProperty<FullBibEntry> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final InMemoryChatHistoryCache chatHistoryCache;

    public AiGroupChatViewModel(AiPreferences aiPreferences, AiService aiService) {
        this.chatHistoryCache = aiService.getChatHistoryCache();

        BooleanExpression databasePathPresent = BooleanExpression.booleanExpression(
                databaseContext.map(BibDatabaseContext::getDatabasePath).map(Optional::isPresent)
        );

        BindingsHelper.bindEnum(
                state,
                State.AI_TURNED_OFF, aiPreferences.enableAiProperty().not(),
                State.NO_DATABASE_PATH, databaseContext.isNotNull().and(databasePathPresent.not()),
                State.CHATTING
        );

        ListenersHelper.onChangeNonNullWhen(
                groupNode, databaseContext,
                aiPreferences.enableAiProperty().and(databasePathPresent),
                this::loadGroupChat
        );
    }

    private void loadGroupChat() {
        BibDatabaseContext context = databaseContext.get();
        GroupNodeViewModel group = groupNode.get();

        assert context.getMetaData().getAiLibraryId().isPresent();

        List<BibEntry> matchedEntries = group.getGroupNode().findMatches(context.getDatabase());
        List<FullBibEntry> matchedEntryIdentifiers = FullBibEntry.fromSeveral(context, matchedEntries);

        entries.set(FXCollections.observableArrayList(matchedEntryIdentifiers));

        chatHistory.set(chatHistoryCache.getForGroup(
                context,
                group.getGroupNode()
        ));
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

    public ListProperty<FullBibEntry> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return chatHistory;
    }
}
