package org.jabref.gui.ai.summary;

import java.time.Instant;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.EndToEndJabGuiTest;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.InMemorySummaryCache;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(ApplicationExtension.class)
class AiSummaryViewModelTest {

    private AiPreferences aiPreferences;
    private SummariesRepository summariesRepository;
    private InMemorySummaryCache inMemoryCache;
    private SummarizationTaskAggregator summarizationTaskAggregator;
    private DialogService dialogService;
    private AiSummaryViewModel viewModel;

    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        aiPreferences = EndToEndJabGuiTest.createAiPreferences(true);
        summariesRepository = mock(SummariesRepository.class);
        inMemoryCache = mock(InMemorySummaryCache.class);
        summarizationTaskAggregator = mock(SummarizationTaskAggregator.class);
        dialogService = mock(DialogService.class);

        viewModel = new AiSummaryViewModel(
                aiPreferences,
                mock(FilePreferences.class),
                summariesRepository,
                inMemoryCache,
                summarizationTaskAggregator,
                dialogService
        );

        databaseContext = new BibDatabaseContext();
    }

    @Test
    void initialStateIsAiTurnedOffWhenAiDisabled() {
        aiPreferences.setAiFeaturesEnabledCurrently(false);

        assertEquals(AiSummaryViewModel.State.AI_TURNED_OFF, viewModel.stateProperty().get());
    }

    @Test
    void stateIsNoFilesWhenEntryHasNoFiles() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("NoFileKey");
        viewModel.entryProperty().set(new FullBibEntry(databaseContext, entry));

        assertEquals(AiSummaryViewModel.State.NO_FILES, viewModel.stateProperty().get());
    }

    @Test
    void stateIsNoSupportedFilesWhenFileTypeIsUnsupported() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("UnsupportedKey")
                .withFiles(List.of(new LinkedFile("archive", "file.zip", "ZIP")));
        viewModel.entryProperty().set(new FullBibEntry(databaseContext, entry));

        assertEquals(AiSummaryViewModel.State.NO_SUPPORTED_FILE_TYPES, viewModel.stateProperty().get());
    }

    @Test
    void stateIsReadyWhenEntryHasPdfFile() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("SupportedKey")
                .withFiles(List.of(new LinkedFile("paper", "file.pdf", "PDF")));
        viewModel.entryProperty().set(new FullBibEntry(databaseContext, entry));

        assertEquals(AiSummaryViewModel.State.READY, viewModel.stateProperty().get());
    }

    @Test
    void stateIsDoneWhenSummaryIsPresent() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("SummaryPresentKey")
                .withFiles(List.of(new LinkedFile("paper", "file.pdf", "PDF")));
        FullBibEntry fullEntry = new FullBibEntry(databaseContext, entry);
        viewModel.entryProperty().set(fullEntry);

        AiMetadata metadata = new AiMetadata(AiProvider.OPEN_AI, "test-model", Instant.now());
        viewModel.summaryProperty().set(new AiSummary(metadata, SummarizatorKind.FULL_DOCUMENT, "Sample summary"));

        assertEquals(AiSummaryViewModel.State.DONE, viewModel.stateProperty().get());
    }

    @Test
    void clearSummaryClearsSummaryAndStateReturnsToReady() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("DeleteSummaryKey")
                .withFiles(List.of(new LinkedFile("paper", "file.pdf", "PDF")));
        FullBibEntry fullEntry = new FullBibEntry(databaseContext, entry);

        AiMetadata metadata = new AiMetadata(AiProvider.OPEN_AI, "test-model", Instant.now());
        AiSummary sampleSummary = new AiSummary(metadata, SummarizatorKind.FULL_DOCUMENT, "Sample summary");
        org.mockito.Mockito.when(inMemoryCache.get(entry)).thenReturn(java.util.Optional.of(sampleSummary));

        viewModel.entryProperty().set(fullEntry);
        assertEquals(AiSummaryViewModel.State.DONE, viewModel.stateProperty().get());

        viewModel.clearSummary(fullEntry);

        assertNull(viewModel.summaryProperty().get());
        assertEquals(AiSummaryViewModel.State.READY, viewModel.stateProperty().get());
        verify(inMemoryCache).remove(entry);
    }
}
