package org.jabref.gui.ai.summary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.gui.DialogService;
import org.jabref.gui.EndToEndJabGuiTest;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class AiSummaryShowingViewModelTest {

    private DialogService dialogService;
    private AiPreferences aiPreferences;
    private FieldPreferences fieldPreferences;
    private BibEntryTypesManager entryTypesManager;
    private AiSummaryShowingViewModel viewModel;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        aiPreferences = EndToEndJabGuiTest.createAiPreferences(true);
        fieldPreferences = mock(FieldPreferences.class);
        entryTypesManager = new BibEntryTypesManager();

        viewModel = new AiSummaryShowingViewModel(aiPreferences, fieldPreferences, entryTypesManager, dialogService);
    }

    @Test
    void webViewSourceEmptyWhenSummaryIsNull() {
        assertEquals("", viewModel.webViewSourceProperty().get());
    }

    @Test
    void webViewSourceUpdatesWithMarkdownAndHtml() {
        AiMetadata metadata = new AiMetadata(AiProvider.OPEN_AI, "gpt-4o", Instant.now());
        AiSummary summary = new AiSummary(metadata, SummarizatorKind.CHUNKED, "# Test Title\nSummary content");
        viewModel.summaryProperty().set(summary);

        viewModel.isMarkdownProperty().set(true);
        assertTrue(viewModel.webViewSourceProperty().get().contains("<h1"));

        viewModel.isMarkdownProperty().set(false);
        assertTrue(viewModel.webViewSourceProperty().get().contains("<body"));
        assertTrue(viewModel.webViewSourceProperty().get().contains("# Test Title"));
    }

    @Test
    void regenerateTriggersHandler() {
        AtomicBoolean called = new AtomicBoolean(false);
        viewModel.onRegenerateProperty().set(_ -> called.set(true));

        viewModel.regenerate();

        assertTrue(called.get());
    }

    @Test
    void regenerateCustomTriggersHandler() {
        AtomicBoolean called = new AtomicBoolean(false);
        viewModel.onRegenerateCustomProperty().set(_ -> called.set(true));

        viewModel.regenerateCustom();

        assertTrue(called.get());
    }

    @Test
    void exportMarkdownShowsNotificationWhenNoSummary() {
        viewModel.exportMarkdown();

        verify(dialogService).notify(anyString());
    }

    @Test
    void exportMarkdownWritesToFile(@TempDir Path tempDir) throws IOException {
        Path exportFile = tempDir.resolve("export.md");
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.of(exportFile));

        AiMetadata metadata = new AiMetadata(AiProvider.OPEN_AI, "gpt-4o", Instant.now());
        AiSummary summary = new AiSummary(metadata, SummarizatorKind.CHUNKED, "Summary text");
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("Smith2024").withField(StandardField.TITLE, "Paper");
        FullBibEntry fullEntry = new FullBibEntry(new BibDatabaseContext(), entry);

        viewModel.summaryProperty().set(summary);
        viewModel.entryProperty().set(fullEntry);

        viewModel.exportMarkdown();

        assertTrue(Files.exists(exportFile));
        assertTrue(Files.readString(exportFile).contains("Summary text"));
        verify(dialogService).notify("Export operation finished successfully.");
    }

    @Test
    void exportJsonWritesToFile(@TempDir Path tempDir) throws IOException {
        Path exportFile = tempDir.resolve("export.json");
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.of(exportFile));

        AiMetadata metadata = new AiMetadata(AiProvider.OPEN_AI, "gpt-4o", Instant.now());
        AiSummary summary = new AiSummary(metadata, SummarizatorKind.CHUNKED, "Summary text");
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("Smith2024").withField(StandardField.TITLE, "Paper");
        FullBibEntry fullEntry = new FullBibEntry(new BibDatabaseContext(), entry);

        viewModel.summaryProperty().set(summary);
        viewModel.entryProperty().set(fullEntry);

        viewModel.exportJson();

        assertTrue(Files.exists(exportFile));
        assertTrue(Files.readString(exportFile).contains("Summary text"));
        verify(dialogService).notify("Export operation finished successfully.");
    }
}
