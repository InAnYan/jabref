package org.jabref.gui.preferences.websearch;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(ApplicationExtension.class)
class WebSearchAiCitationParsingTest {

    private CliPreferences preferences;
    private TaskExecutor taskExecutor;
    private BooleanProperty refAiEnabled;

    @BeforeEach
    void setUp() {
        preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        taskExecutor = new CurrentThreadTaskExecutor();
        refAiEnabled = new SimpleBooleanProperty(false);
    }

    @Test
    void plainCitationParsersExcludesLlmWhenAiIsDisabled() {
        refAiEnabled.set(false);
        WebSearchTabViewModel viewModel = new WebSearchTabViewModel(preferences, refAiEnabled, taskExecutor);

        assertFalse(viewModel.plainCitationParsers().contains(PlainCitationParserChoice.LLM));
    }

    @Test
    void plainCitationParsersIncludesLlmWhenAiIsEnabled() {
        refAiEnabled.set(true);
        WebSearchTabViewModel viewModel = new WebSearchTabViewModel(preferences, refAiEnabled, taskExecutor);

        assertTrue(viewModel.plainCitationParsers().contains(PlainCitationParserChoice.LLM));
    }

    @Test
    void plainCitationParsersDynamicallyTogglesLlmOnAiStateChange() {
        refAiEnabled.set(false);
        WebSearchTabViewModel viewModel = new WebSearchTabViewModel(preferences, refAiEnabled, taskExecutor);
        assertFalse(viewModel.plainCitationParsers().contains(PlainCitationParserChoice.LLM));

        refAiEnabled.set(true);
        assertTrue(viewModel.plainCitationParsers().contains(PlainCitationParserChoice.LLM));

        viewModel.defaultPlainCitationParserProperty().set(PlainCitationParserChoice.LLM);
        assertEquals(PlainCitationParserChoice.LLM, viewModel.defaultPlainCitationParserProperty().get());

        refAiEnabled.set(false);
        assertFalse(viewModel.plainCitationParsers().contains(PlainCitationParserChoice.LLM));
        assertEquals(viewModel.plainCitationParsers().getFirst(), viewModel.defaultPlainCitationParserProperty().get());
    }
}
