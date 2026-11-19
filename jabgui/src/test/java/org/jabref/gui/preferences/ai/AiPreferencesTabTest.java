package org.jabref.gui.preferences.ai;

import java.util.List;

import org.jabref.gui.EndToEndJabGuiTest;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.llm.AiProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiPreferencesTabTest {

    private AiPreferences aiPreferences;
    private AiTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        aiPreferences = EndToEndJabGuiTest.createAiPreferences(true);
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getAiPreferences()).thenReturn(aiPreferences);

        viewModel = new AiTabViewModel(preferences);
    }

    @Test
    void setValuesLoadsCurrentAiPreferences() {
        viewModel.setValues();

        assertTrue(viewModel.enableAi().get());
        assertEquals(aiPreferences.getAutoGenerateEmbeddings(), viewModel.autoGenerateEmbeddings().get());
        assertEquals(aiPreferences.getAutoGenerateSummaries(), viewModel.autoGenerateSummaries().get());
        assertEquals(aiPreferences.getAiProvider(), viewModel.selectedAiProviderProperty().get());
        assertEquals(aiPreferences.getOpenAiChatModel(), viewModel.selectedChatModelProperty().get());
        assertFalse(viewModel.disableBasicSettingsProperty().get());
    }

    @Test
    void disablingAiDisablesBasicSettingsAndExpertSettings() {
        viewModel.setValues();

        viewModel.enableAi().set(false);

        assertTrue(viewModel.disableBasicSettingsProperty().get());
        assertTrue(viewModel.disableExpertSettingsProperty().get());
    }

    @Test
    void togglingAiStateAddsRestartWarningOnStoreSettings() {
        viewModel.setValues();
        assertTrue(viewModel.getRestartWarnings().isEmpty());

        viewModel.enableAi().set(false);
        viewModel.storeSettings();

        assertFalse(aiPreferences.getAiFeaturesEnabledCurrently());
        assertEquals(List.of(Localization.lang("AI features turned on/off.")), viewModel.getRestartWarnings());
    }

    @Test
    void switchingProviderUpdatesChatModelsAndApiKeyBindings() {
        viewModel.setValues();

        viewModel.selectedAiProviderProperty().set(AiProvider.OPEN_AI);
        viewModel.apiKeyProperty().set("test-openai-key");

        viewModel.selectedAiProviderProperty().set(AiProvider.MISTRAL_AI);
        assertEquals("", viewModel.apiKeyProperty().get());

        viewModel.apiKeyProperty().set("test-mistral-key");

        viewModel.selectedAiProviderProperty().set(AiProvider.OPEN_AI);
        assertEquals("test-openai-key", viewModel.apiKeyProperty().get());

        viewModel.selectedAiProviderProperty().set(AiProvider.MISTRAL_AI);
        assertEquals("test-mistral-key", viewModel.apiKeyProperty().get());
    }

    @Test
    void storeSettingsPersistsAllConfiguredAiPreferences() {
        viewModel.setValues();

        viewModel.autoGenerateEmbeddings().set(true);
        viewModel.autoGenerateSummaries().set(true);
        viewModel.generateFollowUpQuestionsProperty().set(true);
        viewModel.followUpQuestionsCountProperty().set(5);

        viewModel.storeSettings();

        assertTrue(aiPreferences.getAutoGenerateEmbeddings());
        assertTrue(aiPreferences.getAutoGenerateSummaries());
        assertTrue(aiPreferences.getGenerateFollowUpQuestions());
        assertEquals(5, aiPreferences.getFollowUpQuestionsCount());
    }
}
