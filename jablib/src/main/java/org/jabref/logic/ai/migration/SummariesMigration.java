
package org.jabref.logic.ai.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.AiSummaryIdentifier;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabaseContext;

import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates summaries from the old v1 MVStore file to the new v2 repository.
 * <p>
 * Old format (v1): Stored in "ai/1/summaries.mv" file
 * Map keys like "summaries-bibDatabasePath" containing Map&lt;String, OldSummary&gt;
 * where OldSummary = record(LocalDateTime timestamp, AiProvider aiProvider, String model, String content)
 * <p>
 * New format (v2): Stored in "ai/2/summaries.mv" file via repository
 * Uses AiSummaryIdentifier(libraryId, citationKey) to store AiSummary objects
 * <p>
 * <b>CHALLENGE:</b> Old Summary used Java serialization with AiProvider at org.jabref.model.ai.AiProvider,
 * but it was moved to org.jabref.model.ai.llm.AiProvider. We use a custom ObjectInputStream to remap the class.
 * <p>
 * Migration strategy: Open old v1 file, read with custom deserializer, write to new v2 repository.
 */
public class SummariesMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummariesMigration.class);

    private static final String OLD_SUMMARIES_FILE_NAME = "summaries.mv";
    private static final String SUMMARIES_MAP_PREFIX = "summaries";
    private static final String OLD_AI_PROVIDER_CLASS = "org.jabref.model.ai.AiProvider";
    private static final String NEW_AI_PROVIDER_CLASS = "org.jabref.model.ai.llm.AiProvider";

    private SummariesMigration() {
        // Utility class
    }

    /**
     * Migrates old summary data from v1 file to v2 repository.
     *
     * @param bibDatabaseContext The database context containing the AI library ID
     * @param repository The new v2 summaries repository
     * @param notificationService Service for notifying user of errors
     */
    public static void migrate(
            BibDatabaseContext bibDatabaseContext,
            SummariesRepository repository,
            NotificationService notificationService
    ) {
        if (bibDatabaseContext.getMetaData().getAiLibraryId().isEmpty()) {
            LOGGER.warn("Cannot migrate summaries: AI library ID is not set");
            return;
        }

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.debug("Cannot migrate summaries: database path is not set");
            return;
        }

        String libraryId = bibDatabaseContext.getMetaData().getAiLibraryId().get();
        Path bibDatabasePath = bibDatabaseContext.getDatabasePath().get();

        // Get path to old v1 MVStore file (in ai/1/ directory)
        Path oldFilePath = Directories.getAiFilesDirectory()
                .getParent()  // Go from ai/2/ to ai/
                .resolve("1")  // Go to ai/1/
                .resolve(OLD_SUMMARIES_FILE_NAME);

        if (!oldFilePath.toFile().exists()) {
            LOGGER.debug("No old summaries file found at {} - skipping migration", oldFilePath);
            return;
        }

        MVStore oldMvStore = null;
        try {
            // Open old v1 MVStore file
            oldMvStore = new MVStore.Builder()
                    .fileName(oldFilePath.toString())
                    .open();

            String oldMapName = SUMMARIES_MAP_PREFIX + "-" + bibDatabasePath.toString();

            if (!oldMvStore.hasMap(oldMapName)) {
                LOGGER.debug("No summaries found for this database in old file");
                return;
            }

            Map<String, byte[]> oldMap = oldMvStore.openMap(oldMapName);

            if (oldMap.isEmpty()) {
                LOGGER.debug("Old summaries map is empty");
                return;
            }

            LOGGER.info("Starting migration of {} summaries from v1 to v2", oldMap.size());

            int migratedCount = 0;
            int failedCount = 0;

            for (Map.Entry<String, byte[]> entry : oldMap.entrySet()) {
                String citationKey = entry.getKey();

                try {
                    OldSummary oldSummary = deserializeOldSummary(entry.getValue());

                    if (oldSummary != null) {
                        AiSummaryIdentifier newIdentifier = new AiSummaryIdentifier(libraryId, citationKey);

                        // Check if already migrated
                        if (repository.get(newIdentifier).isPresent()) {
                            LOGGER.debug("Skipping {} - already has summary in new format", citationKey);
                            continue;
                        }

                        // Convert to new format
                        AiSummary newSummary = convertToNewSummary(oldSummary);
                        repository.set(newIdentifier, newSummary);

                        migratedCount++;
                        LOGGER.debug("Migrated summary for: {}", citationKey);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to migrate summary for {}: {}", citationKey, e.getMessage());
                    failedCount++;
                }
            }

            LOGGER.info("Successfully migrated {} summaries, {} failed", migratedCount, failedCount);

        } catch (Exception e) {
            LOGGER.error("Failed to migrate summaries from v1 to v2", e);
            notificationService.notify(Localization.lang("Failed to migrate AI summaries. See logs for details."));
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

    /**
     * Deserializes old Summary using a custom ObjectInputStream that remaps the old AiProvider class.
     */
    private static OldSummary deserializeOldSummary(byte[] data) {
        try (ClassRemappingObjectInputStream ois = new ClassRemappingObjectInputStream(
                new java.io.ByteArrayInputStream(data))) {
            Object obj = ois.readObject();

            if (obj instanceof OldSummary summary) {
                return summary;
            } else {
                LOGGER.warn("Deserialized object is not OldSummary: {}", obj.getClass());
                return null;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to deserialize old summary: {}", e.getMessage());
            return null;
        }
    }

    private static AiSummary convertToNewSummary(OldSummary oldSummary) {
        Instant timestamp = oldSummary.timestamp().atZone(ZoneId.systemDefault()).toInstant();

        return new AiSummary(
                new AiMetadata(oldSummary.aiProvider(), oldSummary.model(), timestamp),
                SummarizatorKind.CHUNKED,  // Old summaries - assume chunked (default)
                oldSummary.content()
        );
    }

    /**
     * Custom ObjectInputStream that remaps the old AiProvider class name to the new one.
     */
    private static class ClassRemappingObjectInputStream extends ObjectInputStream {
        public ClassRemappingObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass desc = super.readClassDescriptor();

            // Remap old AiProvider class to new location
            if (OLD_AI_PROVIDER_CLASS.equals(desc.getName())) {
                return ObjectStreamClass.lookup(AiProvider.class);
            }

            return desc;
        }
    }

    /**
     * Represents the old Summary format from v1.
     * This must match the structure of the old record.
     */
    private record OldSummary(
            LocalDateTime timestamp,
            AiProvider aiProvider,
            String model,
            String content
    ) implements java.io.Serializable {
    }
}
