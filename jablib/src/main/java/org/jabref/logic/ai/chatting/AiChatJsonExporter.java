package org.jabref.logic.ai.chatting;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports an AI chat conversation to JSON format.
 *
 * <p>The JSON output includes export metadata (provider, model, timestamp), BibTeX entry data,
 * and the conversation messages with role/content pairs.
 */
public class AiChatJsonExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatJsonExporter.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;

    public AiChatJsonExporter(BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences) {
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
    }

    public String export(String aiProvider, String model, List<BibEntry> entries, BibDatabaseMode mode, List<ChatMessage> messages) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("latest_provider", aiProvider);
        root.put("latest_model", model);
        root.put("export_timestamp", Instant.now().toString());

        List<Map<String, Object>> entriesList = new ArrayList<>();
        for (BibEntry entry : entries) {
            Map<String, Object> entryData = new LinkedHashMap<>();

            Map<String, String> fields = new LinkedHashMap<>();
            for (Field field : entry.getFields()) {
                fields.put(field.getName(), entry.getField(field).orElse(""));
            }
            entryData.put("fields", fields);
            entryData.put("bibtex", entryToBibtex(entry, mode));

            entriesList.add(entryData);
        }
        root.put("entries", entriesList);

        List<Map<String, String>> conversationList = new ArrayList<>();
        for (ChatMessage msg : messages) {
            String role = switch (msg.role()) {
                case USER -> "user";
                case AI -> "assistant";
                case ERROR -> "error";
                case SYSTEM -> null; // System messages are not part of the conversation exchange
            };

            if (role == null) {
                continue;
            }

            Map<String, String> message = new LinkedHashMap<>();
            message.put("role", role);
            message.put("content", msg.content());
            conversationList.add(message);
        }
        root.put("conversation", conversationList);

        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            // Should not happen for a plain Map<String, Object> with String values
            throw new RuntimeException("Failed to serialize chat export to JSON", e);
        }
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
