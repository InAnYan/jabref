package org.jabref.logic.ai.chatting;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports an AI chat conversation to Markdown format.
 *
 * <p>The Markdown output is produced by rendering a configurable Velocity template.
 * The template has access to two variables:
 * <ul>
 *   <li>{@code $bibtex} — pre-rendered BibTeX string of all associated entries</li>
 *   <li>{@code $messages} — list of {@link AiTemplateRenderer.ExportMessage} (role + content, system messages excluded)</li>
 * </ul>
 */
public class AiChatMarkdownExporter implements AiChatExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatMarkdownExporter.class);

    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;
    private final String markdownExportTemplate;

    public AiChatMarkdownExporter(BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences, String markdownExportTemplate) {
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
        this.markdownExportTemplate = markdownExportTemplate;
    }

    @Override
    public String export(List<BibEntry> entries, BibDatabaseMode mode, List<ChatMessage> messages) {
        String bibtex = buildBibtex(entries, mode);

        List<AiTemplateRenderer.ExportMessage> exportMessages = messages.stream()
                .filter(msg -> msg.role() != ChatMessage.Role.SYSTEM)
                .map(msg -> {
                    String role = switch (msg.role()) {
                        case USER -> "User";
                        case AI -> "AI";
                        case ERROR -> "Error";
                        case SYSTEM -> throw new AssertionError("SYSTEM filtered above");
                    };
                    return new AiTemplateRenderer.ExportMessage(role, msg.content());
                })
                .toList();

        return AiTemplateRenderer.renderMarkdownChatExport(markdownExportTemplate, bibtex, exportMessages);
    }

    private String buildBibtex(List<BibEntry> entries, BibDatabaseMode mode) {
        StringBuilder sb = new StringBuilder();
        for (BibEntry entry : entries) {
            String bibtex = entryToBibtex(entry, mode).trim();
            if (!bibtex.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(bibtex);
            }
        }
        return sb.toString();
    }

    private String entryToBibtex(BibEntry entry, BibDatabaseMode mode) {
        try {
            StringWriter stringWriter = new StringWriter();
            BibWriter bibWriter = new BibWriter(stringWriter, "\n");
            FieldWriter fieldWriter = FieldWriter.buildIgnoreHashes(fieldPreferences);
            BibEntryWriter bibEntryWriter = new BibEntryWriter(fieldWriter, entryTypesManager);
            bibEntryWriter.write(entry, bibWriter, mode, true);
            return stringWriter.toString();
        } catch (IOException e) {
            LOGGER.error("Could not write entry to BibTeX", e);
            return "";
        }
    }
}
