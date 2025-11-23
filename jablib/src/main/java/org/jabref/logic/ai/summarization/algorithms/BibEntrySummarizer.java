package org.jabref.logic.ai.summarization.algorithms;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.algorithms.FileToDocument;
import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.ai.summarization.Summary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;

public class BibEntrySummarizer {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BibEntrySummarizer.class);

    // TODO: Same thing.
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;

    private final ChunkedSummarizationAlgorithm chunkedSummarizationAlgorithm;

    public BibEntrySummarizer(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            AiTemplatesService aiTemplatesService,
            ChatModel chatModel
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;

        this.chunkedSummarizationAlgorithm = new ChunkedSummarizationAlgorithm(aiPreferences, aiTemplatesService, chatModel);
    }

    public Summary summarize(BibEntry entry, BibDatabaseContext bibDatabaseContext, ProgressCounter progressCounter, ReadOnlyBooleanProperty shutdownSignal) throws InterruptedException {
        String citationKey = entry.getCitationKey().orElse("<no citation key>");

        // Rationale for RuntimeException here:
        // It follows the same idiom as in langchain4j. See {@link JabRefChatLanguageModel.generate}, this method
        // is used internally in the summarization, and it also throws RuntimeExceptions.

        // Stream API would look better here, but we need to catch InterruptedException.
        List<String> linkedFilesSummary = new ArrayList<>();
        for (LinkedFile linkedFile : entry.getFiles()) {
            generateSummary(linkedFile, bibDatabaseContext, citationKey, progressCounter, shutdownSignal)
                    .ifPresent(linkedFilesSummary::add);
        }

        if (linkedFilesSummary.isEmpty()) {
            progressCounter.increaseWorkDone(1); // Skipped generation of final summary.
            throw new RuntimeException(Localization.lang("No summary can be generated for entry '%0'. Could not find attached linked files.", citationKey));
        }

        LOGGER.debug("All summaries for attached files of entry {} are generated. Generating final summary.", citationKey);

        String finalSummary;

        progressCounter.increaseWorkMax(1); // For generating final summary.

        if (linkedFilesSummary.size() == 1) {
            finalSummary = linkedFilesSummary.getFirst();
        } else {
            finalSummary = summarizeSeveralDocuments(linkedFilesSummary.stream(), progressCounter, shutdownSignal);
        }

        progressCounter.increaseWorkDone(1);

        return new Summary(
                LocalDateTime.now(),
                aiPreferences.getAiProvider(),
                aiPreferences.getSelectedChatModel(),
                finalSummary
        );
    }

    private Optional<String> generateSummary(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext, String citationKey, ProgressCounter progressCounter, ReadOnlyBooleanProperty shutdownSignal) throws InterruptedException {
        LOGGER.debug("Generating summary for file \"{}\" of entry {}", linkedFile.getLink(), citationKey);

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\" of entry {}", linkedFile.getLink(), citationKey);
            LOGGER.debug("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        Optional<Document> document = new FileToDocument(shutdownSignal).fromFile(path.get());

        if (document.isEmpty()) {
            LOGGER.warn("Could not extract text from a linked file \"{}\" of entry {}. It will be skipped when generating a summary.", linkedFile.getLink(), citationKey);
            LOGGER.debug("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        String linkedFileSummary = chunkedSummarizationAlgorithm.summarize(document.get().text(), progressCounter, shutdownSignal);

        LOGGER.debug("Summary for file \"{}\" of entry {} was generated successfully", linkedFile.getLink(), citationKey);
        return Optional.of(linkedFileSummary);
    }

    public String summarizeSeveralDocuments(Stream<String> documents, ProgressCounter progressCounter, ReadOnlyBooleanProperty shutdownSignal) throws InterruptedException {
        return chunkedSummarizationAlgorithm.summarize(documents.collect(Collectors.joining("\n\n")), progressCounter, shutdownSignal);
    }
}
