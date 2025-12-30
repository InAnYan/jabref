package org.jabref.gui.ai.chat;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.util.ChatHistoryFactory;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.EntryChatHistoryIdentifier;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class AiEntryChatViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_DATABASE_PATH,
        NO_CITATION_KEY,
        CITATION_KEY_NOT_UNIQUE,
        CHATTING
    }

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.NO_DATABASE_PATH);

    // This is the property that class users should change.
    private final ObjectProperty<BibEntryAiIdentifier> selectedEntry = new SimpleObjectProperty<>();

    // This class sets these properties when a selected entry is changed.
    private final ListProperty<BibEntryAiIdentifier> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ChatHistoryRecordV2> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final AiPreferences aiPreferences;
    private final ChatHistoryRepository chatHistoryRepository;

    public AiEntryChatViewModel(
            AiPreferences aiPreferences,
            ChatHistoryRepository chatHistoryRepository
    ) {
        this.aiPreferences = aiPreferences;
        this.chatHistoryRepository = chatHistoryRepository;

        setupListeners();
    }

    private void setupListeners() {
        // If the AI is disabled, `setEntry` will set the respective state.
        // But if the AI is enabled, `setEntry` will process the selected entry.
        // Thus, it is safe to call it anytime.

        aiPreferences.enableAiProperty().addListener(_ -> {
            if (selectedEntry.get() != null) {
                setEntry(selectedEntry.get());
            }
        });

        ListenersHelper.onChangeNonNull(selectedEntry, this::setEntry);
    }

    private void setEntry(BibEntryAiIdentifier identifier) {
        if (identifier == null) {
            return;
        }

        BibDatabaseContext context = identifier.databaseContext();
        BibEntry entry = identifier.entry();

        if (!aiPreferences.getEnableAi()) {
            state.set(State.AI_TURNED_OFF);
        } else if (context.getDatabasePath().isEmpty()) {
            state.set(State.NO_DATABASE_PATH);
        } else if (entry.getCitationKey().isEmpty() || StringUtil.isBlank(entry.getCitationKey().get())) {
            state.set(State.NO_CITATION_KEY);
        } else if (!CitationKeyCheck.citationKeyIsUnique(context, entry.getCitationKey().get())) {
            state.set(State.CITATION_KEY_NOT_UNIQUE);
        } else {
            entries.set(FXCollections.observableArrayList(identifier));
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

    public ObjectProperty<BibEntryAiIdentifier> selectedEntryProperty() {
        return selectedEntry;
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
}
