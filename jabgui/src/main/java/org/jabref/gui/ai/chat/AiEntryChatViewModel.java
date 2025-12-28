package org.jabref.gui.ai.chat;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.util.ChatHistoryFactory;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.EntryChatHistoryIdentifier;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class AiEntryChatViewModel {
    public enum State {
        NO_DATABASE_PATH,
        NO_CITATION_KEY,
        CITATION_KEY_NOT_UNIQUE,
        CHATTING
    }

    private final ChatHistoryRepository chatHistoryRepository;

    private final ObjectProperty<FullBibEntryAiIdentifier> selectedEntry = new SimpleObjectProperty<>();
    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.NO_DATABASE_PATH);
    private final ListProperty<FullBibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ChatHistoryRecordV2> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    public AiEntryChatViewModel(AiService aiService) {
        this.chatHistoryRepository = aiService.getChattingFeature().getChatHistoryRepository();

        this.selectedEntry.addListener(_ -> {
            setEntry(selectedEntry.get().databaseContext(), selectedEntry.get().entry());
        });
    }

    private void setEntry(BibDatabaseContext context, BibEntry entry) {
        if (context.getDatabasePath().isEmpty()) {
            state.set(State.NO_DATABASE_PATH);
        } else if (entry.getCitationKey().isEmpty() || StringUtil.isBlank(entry.getCitationKey().get())) {
            state.set(State.NO_CITATION_KEY);
        } else if (!CitationKeyCheck.citationKeyIsUnique(context, entry.getCitationKey().get())) {
            state.set(State.CITATION_KEY_NOT_UNIQUE);
        } else {
            entries.set(FXCollections.observableArrayList(new FullBibEntryAiIdentifier(context, entry)));
            chatHistory.set(ChatHistoryFactory.makeChatHistoryProperty(
                    new EntryChatHistoryIdentifier(
                            context.getDatabasePath().get(),
                            entry.getCitationKey().get()
                    ),
                    chatHistoryRepository
            ));
            state.set(State.CHATTING);
        }
    }

    public ObjectProperty<FullBibEntryAiIdentifier> selectedEntryProperty() {
        return selectedEntry;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ListProperty<FullBibEntryAiIdentifier> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatHistoryRecordV2> chatHistoryProperty() {
        return chatHistory;
    }
}
