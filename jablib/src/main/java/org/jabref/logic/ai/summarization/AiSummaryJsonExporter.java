package org.jabref.logic.ai.summarization;

import java.util.List;

import org.jabref.logic.ai.chatting.AiChatJsonExporter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

/**
 * Exports an AI summary to JSON format.
 *
 * <p>Internally constructs a single-message dummy chat containing the summary content
 * and delegates to {@link AiChatJsonExporter}.
 */
public class AiSummaryJsonExporter {
    private final AiChatJsonExporter chatExporter;

    public AiSummaryJsonExporter(BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences) {
        this.chatExporter = new AiChatJsonExporter(entryTypesManager, fieldPreferences);
    }

    public String export(BibEntry entry, BibDatabaseMode mode, AiSummary summary) {
        List<ChatMessage> dummyChat = List.of(ChatMessage.aiMessage(summary.content(), List.of()));
        return chatExporter.export(
                summary.aiProvider().getDisplayName(),
                summary.model(),
                List.of(entry),
                mode,
                dummyChat
        );
    }
}
