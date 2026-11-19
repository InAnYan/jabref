package org.jabref.gui.ai.summary;

import java.util.List;

import org.jabref.gui.EndToEndJabGuiTest;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.summarization.SummarizatorKind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
class AiSummaryParametersViewModelTest {

    private AiPreferences aiPreferences;
    private AiSummaryParametersViewModel viewModel;

    @BeforeEach
    void setUp() {
        aiPreferences = EndToEndJabGuiTest.createAiPreferences(true);
        viewModel = new AiSummaryParametersViewModel(aiPreferences);
    }

    @Test
    void summarizatorKindsContainsAllValues() {
        assertEquals(List.of(SummarizatorKind.values()), viewModel.summarizatorKindsProperty().get());
    }

    @Test
    void summarizatorKindInitializesFromPreferences() {
        assertEquals(aiPreferences.getSummarizatorKind(), viewModel.summarizatorKindProperty().get());
        assertNotNull(viewModel.summarizatorProperty().get());
    }

    @Test
    void summarizatorChangesWhenSummarizatorKindChanges() {
        for (SummarizatorKind kind : SummarizatorKind.values()) {
            viewModel.summarizatorKindProperty().set(kind);
            assertNotNull(viewModel.summarizatorProperty().get());
        }
    }
}
