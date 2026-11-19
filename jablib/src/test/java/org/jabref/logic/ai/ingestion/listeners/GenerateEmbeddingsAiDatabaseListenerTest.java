package org.jabref.logic.ai.ingestion.listeners;

import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GenerateEmbeddingsAiDatabaseListenerTest {

    private AiPreferences aiPreferences;
    private IngestionTaskAggregator ingestionTaskAggregator;
    private BibDatabaseContext databaseContext;
    private BibDatabase database;

    @BeforeEach
    void setUp() {
        aiPreferences = createAiPreferences(true, true);
        ingestionTaskAggregator = mock(IngestionTaskAggregator.class);

        databaseContext = new BibDatabaseContext();
        database = databaseContext.getDatabase();
    }

    private static AiPreferences createAiPreferences(boolean enabled, boolean autoEmbeddings) {
        AiPreferences defaults = AiPreferences.getDefault();
        return new AiPreferences(
                enabled,
                autoEmbeddings,
                defaults.getAutoGenerateSummaries(),
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

    @SuppressWarnings("unchecked")
    private GenerateEmbeddingsAiDatabaseListener createListener() {
        GenerateEmbeddingsAiDatabaseListener listener = new GenerateEmbeddingsAiDatabaseListener(
                aiPreferences,
                mock(FilePreferences.class),
                mock(IngestedDocumentsRepository.class),
                mock(EmbeddingStore.class),
                mock(EmbeddingModelCache.class),
                ingestionTaskAggregator
        );
        listener.setupDatabase(databaseContext);
        return listener;
    }

    @Test
    void autoGenerateEmbeddingsTriggersWhenEntryWithPdfAdded() {
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntryEmbeddings2026")
                .withFiles(List.of(new LinkedFile("paper", "paper.pdf", "PDF")));

        database.insertEntry(entry);

        verify(ingestionTaskAggregator).start(any(GenerateEmbeddingsTaskRequest.class));
    }

    @Test
    void autoGenerateEmbeddingsDoesNotTriggerWhenDisabled() {
        aiPreferences = createAiPreferences(true, false);
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntryNoEmbeddings2026")
                .withFiles(List.of(new LinkedFile("paper", "paper.pdf", "PDF")));

        database.insertEntry(entry);

        verify(ingestionTaskAggregator, never()).start(any(GenerateEmbeddingsTaskRequest.class));
    }

    @Test
    void autoGenerateEmbeddingsDoesNotTriggerForEntryWithoutFiles() {
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntryWithoutFiles2026");

        database.insertEntry(entry);

        verify(ingestionTaskAggregator, never()).start(any(GenerateEmbeddingsTaskRequest.class));
    }

    @Test
    void autoGenerateEmbeddingsTriggersWhenFileFieldChanges() {
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntryFieldChange2026");
        database.insertEntry(entry);

        verify(ingestionTaskAggregator, never()).start(any(GenerateEmbeddingsTaskRequest.class));

        entry.setFiles(List.of(new LinkedFile("newPaper", "newPaper.pdf", "PDF")));

        verify(ingestionTaskAggregator).start(any(GenerateEmbeddingsTaskRequest.class));
    }

    @Test
    void autoGenerateEmbeddingsDoesNotTriggerWhenUnrelatedFieldChanges() {
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntryTitleChange2026")
                .withFiles(List.of(new LinkedFile("paper", "paper.pdf", "PDF")));
        database.insertEntry(entry);

        verify(ingestionTaskAggregator).start(any(GenerateEmbeddingsTaskRequest.class));

        entry.setField(StandardField.TITLE, "New Title");

        verify(ingestionTaskAggregator).start(any(GenerateEmbeddingsTaskRequest.class));
    }

    @Test
    void autoGenerateEmbeddingsDoesNotTriggerWhenAiDisabled() {
        aiPreferences = createAiPreferences(false, true);
        createListener();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("EntryAiDisabledEmbeddings2026")
                .withFiles(List.of(new LinkedFile("paper", "paper.pdf", "PDF")));

        database.insertEntry(entry);

        verify(ingestionTaskAggregator, never()).start(any(GenerateEmbeddingsTaskRequest.class));
    }
}
