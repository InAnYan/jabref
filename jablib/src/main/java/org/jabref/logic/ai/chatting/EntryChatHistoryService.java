package org.jabref.logic.ai.chatting;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.repositories.EntryChatHistoryRepository;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.util.CitationKeyCheck;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.identifiers.GroupAiIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.groups.GroupTreeNode;

import com.google.common.eventbus.Subscribe;
import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Main class for getting and storing chat history for entries and groups.
/// Use this class <s>in logic and</s> UI.
/// It currently resides in the UI package because it relies on the `org.jabref.gui.StateManager` to get the open databases and to find the correct [BibDatabaseContext] based on an entry.
///
/// The returned chat history is a [ObservableList]. So chat history exists for every possible
/// [BibEntry] and [org.jabref.model.groups.AbstractGroup]. The chat history is stored in runtime.
///
/// To save and load chat history, [BibEntry] and [org.jabref.model.groups.AbstractGroup] must satisfy several constraints.
/// Serialization and deserialization is handled in [EntryChatHistoryRepository].
///
/// Constraints for serialization and deserialization of a chat history of a [BibEntry]:
/// 1. There should exist an associated [BibDatabaseContext] for the [BibEntry].
/// 2. The database path of the associated [BibDatabaseContext] must be set.
/// 3. The citation key of the [BibEntry] must be set and unique.
///
/// Constraints for serialization and deserialization of a chat history of an [GroupTreeNode]:
/// 1. There should exist an associated [BibDatabaseContext] for the [GroupTreeNode].
/// 2. The database path of the associated [BibDatabaseContext] must be set.
/// 3. The name of an [GroupTreeNode] must be set and unique (this requirement is possibly already satisfied in
///    JabRef, but for [BibEntry] it is definitely not).
public class EntryChatHistoryService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntryChatHistoryService.class);

    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final EntryChatHistoryRepository entryChatHistoryRepository;

    // We use a {@link TreeMap} here to store {@link BibEntry} chat histories by their id.
    // When you compare {@link BibEntry} instances, they are compared by value, not by reference.
    // And when you store {@link BibEntry} instances in a {@link HashMap}, an old hash may be stored when the {@link BibEntry} is changed.
    // See also ADR-38.
    private final TreeMap<BibEntry, ChatHistoryManagementRecord> bibEntriesChatHistory =
            new TreeMap<>(Comparator.comparing(BibEntry::getId));

    public EntryChatHistoryService(
            CitationKeyPatternPreferences citationKeyPatternPreferences,
            EntryChatHistoryRepository entryChatHistoryRepository
    ) {
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.entryChatHistoryRepository = entryChatHistoryRepository;
    }

    public void setupDatabase(BibDatabaseContext bibDatabaseContext) {
        bibDatabaseContext
                .getDatabase()
                .getEntries()
                .forEach(entry ->
                        entry.registerListener(new CitationKeyChangeListener(bibDatabaseContext))
                );
    }

    public ObservableList<ChatMessage> getChatHistory(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        return bibEntriesChatHistory.computeIfAbsent(entry, entryArg -> {
            ObservableList<ChatMessage> chatHistory;

            if (entry.getCitationKey().isEmpty() || !correctCitationKey(bibDatabaseContext, entry) || bibDatabaseContext.getDatabasePath().isEmpty()) {
                chatHistory = FXCollections.observableArrayList();
            } else {
                BibEntryAiIdentifier identifier = new BibEntryAiIdentifier(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get());
                List<ChatMessage> chatMessagesList = entryChatHistoryRepository.loadMessagesForEntry(identifier);
                chatHistory = FXCollections.observableArrayList(chatMessagesList);
            }

            return new ChatHistoryManagementRecord(Optional.of(bibDatabaseContext), chatHistory);
        }).chatHistory();
    }

    /**
     * Removes the chat history for the given {@link BibEntry} from the internal RAM map.
     * If the {@link BibEntry} satisfies requirements for serialization and deserialization of chat history (see
     * the docstring for the {@link EntryChatHistoryService}), then the chat history will be stored via the
     * {@link EntryChatHistoryRepository}.
     * <p>
     * It is not necessary to call this method (everything will be stored in {@link EntryChatHistoryService#close()},
     * but it's best to call it when the chat history {@link BibEntry} is no longer needed.
     */
    public void closeChatHistory(BibEntry entry) {
        ChatHistoryManagementRecord chatHistoryManagementRecord = bibEntriesChatHistory.get(entry);
        if (chatHistoryManagementRecord == null) {
            return;
        }

        Optional<BibDatabaseContext> bibDatabaseContext = chatHistoryManagementRecord.bibDatabaseContext();

        if (bibDatabaseContext.isPresent() && entry.getCitationKey().isPresent() && correctCitationKey(bibDatabaseContext.get(), entry) && bibDatabaseContext.get().getDatabasePath().isPresent()) {
            // Method `correctCitationKey` will already check `entry.getCitationKey().isPresent()`, but it is still
            // there, to suppress warning from IntelliJ IDEA on `entry.getCitationKey().get()`.
            BibEntryAiIdentifier identifier = new BibEntryAiIdentifier(bibDatabaseContext.get().getDatabasePath().get(), entry.getCitationKey().get());
            entryChatHistoryRepository.storeMessagesForEntry(
                    identifier,
                    chatHistoryManagementRecord.chatHistory()
            );
        }

        // TODO: What if there is two AI chats for the same entry? And one is closed and one is not?
        bibEntriesChatHistory.remove(entry);
    }

    private boolean correctCitationKey(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        if (!CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, bibEntry)) {
            tryToGenerateCitationKey(bibDatabaseContext, bibEntry);
        }

        return CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, bibEntry);
    }

    private void tryToGenerateCitationKey(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences).generateAndSetKey(bibEntry);
    }

    @Override
    public void close() throws Exception {
        // We need to clone `bibEntriesChatHistory.keySet()` because closeChatHistoryForEntry() modifies the `bibEntriesChatHistory` map.
        new HashSet<>(bibEntriesChatHistory.keySet()).forEach(this::closeChatHistory);

        // TODO: IT DOES NOT HAVE THE RIGHT TO CLOSE THIS.
        entryChatHistoryRepository.close();
    }

    private void transferHistory(BibDatabaseContext bibDatabaseContext, BibEntry entry, String oldCitationKey, String newCitationKey) {
        // TODO: This method does not check if the citation key is valid.

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Could not transfer chat history of entry {} (old key: {}): database path is empty.", newCitationKey, oldCitationKey);
            return;
        }

        List<ChatMessage> chatMessages = bibEntriesChatHistory.computeIfAbsent(entry,
                e -> new ChatHistoryManagementRecord(Optional.of(bibDatabaseContext), FXCollections.observableArrayList())).chatHistory();

        GroupAiIdentifier groupIdentifier = new GroupAiIdentifier(bibDatabaseContext.getDatabasePath().get(), oldCitationKey);
        BibEntryAiIdentifier bibEntryIdentifier = new BibEntryAiIdentifier(bibDatabaseContext.getDatabasePath().get(), newCitationKey);

        // TODO: Why group here?
        // implementation.storeMessagesForGroup(groupIdentifier, List.of());
        entryChatHistoryRepository.storeMessagesForEntry(bibEntryIdentifier, chatMessages);
    }

    private class CitationKeyChangeListener {
        private final BibDatabaseContext bibDatabaseContext;

        public CitationKeyChangeListener(BibDatabaseContext bibDatabaseContext) {
            this.bibDatabaseContext = bibDatabaseContext;
        }

        @Subscribe
        void listen(FieldChangedEvent e) {
            if (e.getField() != InternalField.KEY_FIELD) {
                return;
            }

            transferHistory(bibDatabaseContext, e.getBibEntry(), e.getOldValue(), e.getNewValue());
        }
    }
}
