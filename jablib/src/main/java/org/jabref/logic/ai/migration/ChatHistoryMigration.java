package org.jabref.logic.ai.migration;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.chatting.ChatHistoryRecord;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.chatting.ChatType;
import org.jabref.model.ai.chatting.ErrorMessage;
import org.jabref.model.database.BibDatabaseContext;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates chat history from the old v1 MVStore file to the new v2 repository.
 * <p>
 * Old format (v1): Stored in "chat-history.mv" file (singular)
 * Map keys like "bibDatabasePath-entry-citationKey" or "bibDatabasePath-group-groupName"
 * containing Map&lt;Integer, ChatHistoryRecord&gt;
 * <p>
 * New format (v2): Stored in "chat-histories.mv" file via repository
 * Uses ChatIdentifier(libraryId, chatType, chatName) to store ChatMessage objects
 * <p>
 * Migration strategy: Open old v1 MVStore file, read old data, write to new repository via interface, close old file.
 */
public class ChatHistoryMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryMigration.class);

    private static final String OLD_CHAT_HISTORY_FILE_NAME = "chat-history.mv";  // Old v1 file (singular)
    private static final String ENTRY_CHAT_HISTORY_INFIX = "-entry-";
    private static final String GROUP_CHAT_HISTORY_INFIX = "-group-";

    private ChatHistoryMigration() {
        // Utility class
    }

    /**
     * Migrates old chat history data from v1 file to v2 repository.
     *
     * @param bibDatabaseContext The database context containing the AI library ID
     * @param repository The new v2 chat history repository to migrate to
     * @param notificationService Service for notifying user of errors
     */
    public static void migrate(
            BibDatabaseContext bibDatabaseContext,
            ChatHistoryRepository repository,
            NotificationService notificationService
    ) {
        if (bibDatabaseContext.getMetaData().getAiLibraryId().isEmpty()) {
            LOGGER.warn("Cannot migrate chat history: AI library ID is not set");
            return;
        }

        String libraryId = bibDatabaseContext.getMetaData().getAiLibraryId().get();

        // Get path to old v1 MVStore file (in ai/1/ directory)
        Path oldFilePath = Directories.getAiFilesDirectory()
                .getParent()  // Go from ai/2/ to ai/
                .resolve("1")  // Go to ai/1/
                .resolve(OLD_CHAT_HISTORY_FILE_NAME);

        if (!oldFilePath.toFile().exists()) {
            LOGGER.debug("No old chat history file found at {} - skipping migration", oldFilePath);
            return;
        }

        MVStore oldMvStore = null;
        try {
            // Open old v1 MVStore file
            oldMvStore = new MVStore.Builder()
                    .fileName(oldFilePath.toString())
                    .open();

            List<String> oldMapNames = new ArrayList<>();
            List<String> migratedMapNames = new ArrayList<>();

            // Collect all old map names
            for (String mapName : oldMvStore.getMapNames()) {
                if (isOldChatHistoryMap(mapName)) {
                    oldMapNames.add(mapName);
                }
            }

            if (oldMapNames.isEmpty()) {
                LOGGER.debug("No old chat history data found for migration");
                return;
            }

            LOGGER.info("Starting migration of {} chat history maps from v1 to v2", oldMapNames.size());

            // Migrate each old map
            for (String oldMapName : oldMapNames) {
                try {
                    // Read from old v1 MVStore, write through new v2 repository interface
                    if (migrateOldMap(oldMapName, libraryId, bibDatabaseContext, repository, oldMvStore)) {
                        migratedMapNames.add(oldMapName);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to migrate chat history map: {}", oldMapName, e);
                }
            }

            LOGGER.info("Successfully migrated {} of {} chat history maps", migratedMapNames.size(), oldMapNames.size());

            // Note: We don't delete the old file here - let the user do it manually after verification
            LOGGER.info("Old chat history file retained at: {}", oldFilePath);
            LOGGER.info("You can manually delete it after verifying the migration was successful");

        } catch (Exception e) {
            LOGGER.error("Failed to migrate chat history from v1 to v2", e);
            notificationService.notify(Localization.lang("Failed to migrate AI chat history. See logs for details."));
        } finally {
            if (oldMvStore != null) {
                try {
                    oldMvStore.close();
                } catch (Exception e) {
                    LOGGER.error("Error closing old MVStore", e);
                }
            }
        }
    }

    private static boolean isOldChatHistoryMap(String mapName) {
        return mapName.contains(ENTRY_CHAT_HISTORY_INFIX) || mapName.contains(GROUP_CHAT_HISTORY_INFIX);
    }

    private static boolean migrateOldMap(
            String oldMapName,
            String libraryId,
            BibDatabaseContext bibDatabaseContext,
            ChatHistoryRepository repository,
            MVStore oldMvStore
    ) {
        ChatType chatType;
        String chatName;

        // Parse old map name to extract chat type and name
        if (oldMapName.contains(ENTRY_CHAT_HISTORY_INFIX)) {
            chatType = ChatType.WITH_ENTRY;
            int index = oldMapName.lastIndexOf(ENTRY_CHAT_HISTORY_INFIX);
            chatName = oldMapName.substring(index + ENTRY_CHAT_HISTORY_INFIX.length());

            // Check if entry with this citation key exists
            if (bibDatabaseContext.getDatabase().getEntriesByCitationKey(chatName).isEmpty()) {
                LOGGER.debug("Skipping chat history migration for non-existent entry: {}", chatName);
                return false;
            }
        } else if (oldMapName.contains(GROUP_CHAT_HISTORY_INFIX)) {
            chatType = ChatType.WITH_GROUP;
            int index = oldMapName.lastIndexOf(GROUP_CHAT_HISTORY_INFIX);
            chatName = oldMapName.substring(index + GROUP_CHAT_HISTORY_INFIX.length());
        } else {
            LOGGER.warn("Unknown chat history map format: {}", oldMapName);
            return false;
        }

        // Read old chat history records from old v1 MVStore
        Map<Integer, ChatHistoryRecord> oldMap = oldMvStore.openMap(oldMapName);

        if (oldMap.isEmpty()) {
            LOGGER.debug("Skipping empty chat history map: {}", oldMapName);
            return true;
        }

        ChatIdentifier newIdentifier = new ChatIdentifier(libraryId, chatType, chatName);

        // Check if new format already has data (avoid overwriting)
        if (!repository.isEmpty(newIdentifier)) {
            LOGGER.debug("Skipping migration for {} - new format already has data", oldMapName);
            return false;
        }

        // Convert and store messages using new v2 repository interface
        List<ChatMessage> newMessages = oldMap.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .map(ChatHistoryMigration::convertToNewChatMessage)
                .toList();

        for (ChatMessage message : newMessages) {
            repository.addMessage(newIdentifier, message);
        }

        LOGGER.debug("Migrated {} messages from {}", newMessages.size(), oldMapName);
        return true;
    }

    private static ChatMessage convertToNewChatMessage(ChatHistoryRecord oldRecord) {
        dev.langchain4j.data.message.ChatMessage langchainMessage = oldRecord.toLangchainMessage();

        ChatMessage.Role role;
        String content;

        switch (langchainMessage) {
            case AiMessage aiMessage -> {
                role = ChatMessage.Role.AI;
                content = aiMessage.text();
            }
            case UserMessage userMessage -> {
                role = ChatMessage.Role.USER;
                content = userMessage.singleText();
            }
            case ErrorMessage errorMessage -> {
                role = ChatMessage.Role.ERROR;
                content = errorMessage.getText();
            }
            default -> {
                LOGGER.warn("Unknown message type during migration: {}", langchainMessage.getClass().getName());
                role = ChatMessage.Role.AI;
                content = "";
            }
        }

        return new ChatMessage(
                UUID.randomUUID().toString(),
                Instant.now(),
                role,
                content,
                List.of()
        );
    }
}



