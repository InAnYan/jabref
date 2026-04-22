package org.jabref.gui.ai.chat;

import java.util.Map;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.tobiasdiez.easybind.EasyBind;

public class AiEntryChatViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        CHATTING
    }

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.AI_TURNED_OFF);
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

        BindingsHelper.bindEnum(
                state,
                State.CHATTING,

                Map.entry(State.AI_TURNED_OFF,
                        isAiTurnedOff.orElse(true)
                )
        );
    }

    private void setupListeners() {
        EasyBind.subscribe(selectedEntry, this::load);
    }

    private void load(FullBibEntry identifier) {
        if (selectedEntry.get() == null || state.get() != State.CHATTING) {
            return;
        }
        
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
