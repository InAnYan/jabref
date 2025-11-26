package org.jabref.logic.ai.summarization.logic;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.rag.logic.parsing.UniversalFileParser;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.SummarizationAlgorithm;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatModelInfo;
import org.jabref.model.ai.summarization.BibEntrySummary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;

public class BibEntrySummarizer {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BibEntrySummarizer.class);

    private final FilePreferences filePreferences;

    private final SummarizationAlgorithm summarizationAlgorithm;
    private final UniversalFileParser universalFileParser = new UniversalFileParser();

    public BibEntrySummarizer(
            FilePreferences filePreferences,
            SummarizationAlgorithm summarizationAlgorithm
    ) {
        this.filePreferences = filePreferences;
        this.summarizationAlgorithm = summarizationAlgorithm;
    }

    public BibEntrySummary summarize(
            ChatModelInfo chatModelInfo,
            LongTaskInfo longTaskInfo,
            BibDatabaseContext bibDatabaseContext,
            BibEntry entry
    ) throws InterruptedException {
        String citationKey = entry.getCitationKey().orElse("<no citation key>");

        // Rationale for RuntimeException here:
        // It follows the same idiom as in langchain4j. See {@link JabRefChatLanguageModel.generate}, this method
        // is used internally in the summarization, and it also throws RuntimeExceptions.

        // Stream API would look better here, but we need to catch InterruptedException.
        List<String> linkedFilesSummary = new ArrayList<>();
        for (LinkedFile linkedFile : entry.getFiles()) {
            generateSummary(
                    chatModelInfo,
                    longTaskInfo,
                    bibDatabaseContext,
                    linkedFile,
                    citationKey
            )
                    .ifPresent(linkedFilesSummary::add);
        }

        if (linkedFilesSummary.isEmpty()) {
            longTaskInfo.progressCounter().increaseWorkDone(1); // Skipped generation of final summary.
            throw new RuntimeException(Localization.lang("No summary can be generated for entry '%0'. Could not find attached linked files.", citationKey));
        }

        LOGGER.debug("All summaries for attached files of entry {} are generated. Generating final summary.", citationKey);

        String finalSummary;

        longTaskInfo.progressCounter().increaseWorkMax(1); // For generating final summary.

        if (linkedFilesSummary.size() == 1) {
            finalSummary = linkedFilesSummary.getFirst();
        } else {
            finalSummary = summarizeSeveralDocuments(
                    chatModelInfo,
                    longTaskInfo,
                    linkedFilesSummary.stream()
            );
        }

        longTaskInfo.progressCounter().increaseWorkDone(1);

        return new BibEntrySummary(
                LocalDateTime.now(),
                chatModelInfo.aiProvider(),
                chatModelInfo.name(),
                summarizationAlgorithm.getName(),
                finalSummary
        );
    }

    private Optional<String> generateSummary(
            ChatModelInfo chatModelInfo,
            LongTaskInfo longTaskInfo,
            BibDatabaseContext bibDatabaseContext,
            LinkedFile linkedFile,
            String citationKey
    ) throws InterruptedException {
        LOGGER.debug("Generating summary for file \"{}\" of entry {}", linkedFile.getLink(), citationKey);

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\" of entry {}", linkedFile.getLink(), citationKey);
            LOGGER.debug("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        Optional<String> document = universalFileParser.parse(path.get(), longTaskInfo.shutdownSignal());

        if (document.isEmpty()) {
            LOGGER.warn("Could not extract text from a linked file \"{}\" of entry {}. It will be skipped when generating a summary.", linkedFile.getLink(), citationKey);
            LOGGER.debug("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        String linkedFileSummary = summarizationAlgorithm.summarize(
                chatModelInfo,
                longTaskInfo,
                document.get()
        );

        LOGGER.debug("BibEntrySummary for file \"{}\" of entry {} was generated successfully", linkedFile.getLink(), citationKey);
        return Optional.of(linkedFileSummary);
    }

    public String summarizeSeveralDocuments(
            ChatModelInfo chatModelInfo,
            LongTaskInfo longTaskInfo,
            Stream<String> documents
    ) throws InterruptedException {
        return summarizationAlgorithm.summarize(
                chatModelInfo,
                longTaskInfo,
                documents.collect(Collectors.joining("\n\n"))
        );
    }
}
