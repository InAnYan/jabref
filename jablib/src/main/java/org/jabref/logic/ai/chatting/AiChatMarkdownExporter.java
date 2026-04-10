package org.jabref.logic.ai.chatting;

import java.util.List;
import java.util.StringJoiner;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

/**
 * Exports an AI chat conversation to Markdown format.
 *
 * <p>The Markdown output contains a BibTeX section with entries and a conversation section
 * with the chat messages formatted as User/AI/Error turns.
 */
public class AiChatMarkdownExporter {
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;

    public AiChatMarkdownExporter(BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences) {
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
    }

    public String export(List<BibEntry> entries, BibDatabaseMode mode, List<ChatMessage> messages) {
        StringJoiner stringJoiner = new StringJoiner("\n");
        stringJoiner.add("# AI chat");
        stringJoiner.add("");
        stringJoiner.add("## BibTeX");
        stringJoiner.add("");
        stringJoiner.add("```bibtex");

        StringJoiner bibtexJoiner = new StringJoiner("\n");
        for (BibEntry entry : entries) {
            String bibtex = entry.getStringRepresentation(entry, mode, entryTypesManager, fieldPreferences).trim();
            if (!bibtex.isEmpty()) {
                bibtexJoiner.add(bibtex);
                bibtexJoiner.add("");
            }
        }
        stringJoiner.add(bibtexJoiner.toString().trim());

        stringJoiner.add("```");
        stringJoiner.add("");
        stringJoiner.add("## Conversation");
        stringJoiner.add("");

        StringJoiner conversation = new StringJoiner("\n");
        for (ChatMessage msg : messages) {
            String role = switch (msg.role()) {
                case USER -> "User";
                case AI -> "AI";
                case ERROR -> "Error";
                case SYSTEM -> null; // System messages are not part of the conversation exchange
            };

            if (role == null) {
                continue;
            }

            conversation.add("**" + role + ":**");
            conversation.add("");
            conversation.add(msg.content());
            conversation.add("");
        }
        stringJoiner.add(conversation.toString().trim());

        return stringJoiner + "\n";
    }
}
