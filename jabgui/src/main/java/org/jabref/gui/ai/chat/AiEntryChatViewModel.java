package org.jabref.gui.ai.chat;

import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
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
    private final ObjectProperty<FullBibEntry> selectedEntry = new SimpleObjectProperty<>();
    private final ListProperty<FullBibEntry> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final AiPreferences aiPreferences;
    private final InMemoryChatHistoryCache chatHistoryCache;

    public AiEntryChatViewModel(
            AiPreferences aiPreferences,
            InMemoryChatHistoryCache chatHistoryCache
    ) {
        this.aiPreferences = aiPreferences;
        this.chatHistoryCache = chatHistoryCache;

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        ObservableValue<Boolean> isAiTurnedOff = aiPreferences.enableAiProperty().not();

        ObservableValue<Boolean> isNoDatabasePath = selectedEntry
                .map(FullBibEntry::databaseContext)
                .map(BibDatabaseContext::getDatabasePath)
                .map(Optional::isEmpty)
                .orElse(false);

        ObservableValue<Boolean> isNoCitationKey = selectedEntry
                .map(FullBibEntry::entry)
                .map(BibEntry::getCitationKey)
                .map(opt -> opt.isEmpty() || StringUtil.isBlank(opt.get()))
                .orElse(false);

        ObservableValue<Boolean> isCitationKeyNotUnique = selectedEntry
                .map(CitationKeyCheck::citationKeyIsUnique)
                .map(unique -> !unique)
                .orElse(false);

        BindingsHelper.bindEnum(
                state,
                State.AI_TURNED_OFF, isAiTurnedOff,
                State.NO_DATABASE_PATH, isNoDatabasePath,
                State.NO_CITATION_KEY, isNoCitationKey,
                State.CITATION_KEY_NOT_UNIQUE, isCitationKeyNotUnique,
                State.CHATTING
        );
    }

    private void setupListeners() {
        ListenersHelper.onChangeNonNullWhen(
                selectedEntry,
                selectedEntry.isNotNull().and(state.isEqualTo(State.CHATTING)),
                this::load
        );
    }

    // [impl->req~ai.chat.entries.ui~1]
    private void load(FullBibEntry identifier) {
        assert identifier.databaseContext().getMetaData().getAiLibraryId().isPresent();
        assert identifier.entry().getCitationKey().isPresent();

        entries.set(FXCollections.observableArrayList(identifier));

        chatHistory.set(chatHistoryCache.getForEntry(
                identifier.databaseContext(),
                identifier.entry()
        ));
    }

    public ObjectProperty<FullBibEntry> selectedEntryProperty() {
        return selectedEntry;
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
