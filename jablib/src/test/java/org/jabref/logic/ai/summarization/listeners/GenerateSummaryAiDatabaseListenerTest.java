package org.jabref.logic.ai.summarization.listeners;

import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTaskRequest;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GenerateSummaryAiDatabaseListenerTest {

    private AiPreferences aiPreferences;
    private SummarizationTaskAggregator summarizationTaskAggregator;
    private BibDatabaseContext databaseContext;
    private BibDatabase database;

    @BeforeEach
    void setUp() {
        aiPreferences = createAiPreferences(true, true);
        summarizationTaskAggregator = mock(SummarizationTaskAggregator.class);

        databaseContext = new BibDatabaseContext();
        database = databaseContext.getDatabase();
    }

    private static AiPreferences createAiPreferences(boolean enabled, boolean autoSummaries) {
        AiPreferences defaults = AiPreferences.getDefault();
        return new AiPreferences(
                enabled,
                defaults.getAutoGenerateEmbeddings(),
                autoSummaries,
                defaults.getAiProvider(),
                defaults.getOpenAiChatModel(),
                defaults.getMistralAiChatModel(),
                defaults.getGeminiChatModel(),
                defaults.getHuggingFaceChatModel(),
                defaults.getCustomizeExpertSettings(),
                defaults.getOpenAiApiBaseUrl(),
                defaults.getMistralAiApiBaseUrl(),
                defaults.getGeminiApiBaseUrl(),
                defaults.getHuggingFaceApiBaseUrl(),
                defaults.getSummarizatorKind(),
                defaults.getTokenEstimatorKind(),
                defaults.embeddingModelProperty().get(),
                defaults.temperatureProperty().get(),
                defaults.contextWindowSizeProperty().get(),
                defaults.getDocumentSplitterKind(),
                defaults.documentSplitterChunkSizeProperty().get(),
                defaults.documentSplitterOverlapSizeProperty().get(),
                defaults.getAnswerEngineKind(),
                defaults.ragMaxResultsCountProperty().get(),
                defaults.ragMinScoreProperty().get(),
                defaults.getChattingSystemMessageTemplate(),
                defaults.getChattingUserMessageTemplate(),
                defaults.getSummarizationChunkSystemMessageTemplate(),
                defaults.getSummarizationCombineSystemMessageTemplate(),
                defaults.getSummarizationFullDocumentSystemMessageTemplate(),
                defaults.getCitationParsingSystemMessageTemplate(),
                defaults.getMarkdownChatExportTemplate(),
                defaults.getGenerateFollowUpQuestions(),
                defaults.getFollowUpQuestionsCount(),
                defaults.getFollowUpQuestionsTemplate()
        );
    }

    private GenerateSummaryAiDatabaseListener createListener() {
        GenerateSummaryAiDatabaseListener listener = new GenerateSummaryAiDatabaseListener(
                aiPreferences,
                mock(FilePreferences.class),
                summarizationTaskAggregator
        );
        listener.setupDatabase(databaseContext);
        return listener;
    }

    @Test
    void autoGenerateSummariesTriggersWhenEntryAdded() {
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntrySummary2026");

        database.insertEntry(entry);

        verify(summarizationTaskAggregator).start(any(GenerateSummaryTaskRequest.class));
    }

    @Test
    void autoGenerateSummariesDoesNotTriggerWhenDisabled() {
        aiPreferences = createAiPreferences(true, false);
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntryNoSummary2026");

        database.insertEntry(entry);

        verify(summarizationTaskAggregator, never()).start(any(GenerateSummaryTaskRequest.class));
    }

    @Test
    void autoGenerateSummariesTriggersWhenFileFieldChanges() {
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntrySummaryFileChange2026");
        database.insertEntry(entry);

        verify(summarizationTaskAggregator).start(any(GenerateSummaryTaskRequest.class));

        entry.setFiles(List.of(new LinkedFile("paper", "paper.pdf", "PDF")));

        verify(summarizationTaskAggregator, times(2)).start(any(GenerateSummaryTaskRequest.class));
    }

    @Test
    void autoGenerateSummariesDoesNotTriggerWhenAiFeaturesTurnedOff() {
        aiPreferences = createAiPreferences(false, true);
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntryAiDisabledSummary2026");

        database.insertEntry(entry);

        verify(summarizationTaskAggregator, never()).start(any(GenerateSummaryTaskRequest.class));
    }
}
