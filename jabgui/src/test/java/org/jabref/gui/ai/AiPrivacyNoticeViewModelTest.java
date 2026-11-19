package org.jabref.gui.ai;

import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(ApplicationExtension.class)
class AiPrivacyNoticeViewModelTest {

    private AiPreferences aiPreferences;
    private EntryEditorPreferences entryEditorPreferences;
    private GroupsPreferences groupsPreferences;
    private DialogService dialogService;
    private AiPrivacyNoticeViewModel viewModel;

    @BeforeEach
    void setUp() {
        aiPreferences = AiPreferences.getDefault();
        aiPreferences.setAiFeaturesEnabledCurrently(false);

        entryEditorPreferences = EntryEditorPreferences.getDefault();
        groupsPreferences = mock(GroupsPreferences.class);
        dialogService = mock(DialogService.class);

        viewModel = new AiPrivacyNoticeViewModel(
                aiPreferences,
                mock(ExternalApplicationsPreferences.class),
                entryEditorPreferences,
                groupsPreferences,
                dialogService
        );
    }

    @Test
    void agreeingToPrivacyPolicyEnablesAiFeatures() {
        assertFalse(aiPreferences.getAiFeaturesEnabledCurrently());

        viewModel.onPrivacyAgree();

        assertTrue(aiPreferences.getAiFeaturesEnabledCurrently());
    }

    @Test
    void disagreeingWithPrivacyPolicyHidesAiTabsAndDisablesAi() {
        aiPreferences.setAiFeaturesEnabledCurrently(true);
        entryEditorPreferences.setTabVisible(EntryEditorTabModel.BuiltIn.AI_CHAT, true);
        entryEditorPreferences.setTabVisible(EntryEditorTabModel.BuiltIn.AI_SUMMARY, true);

        viewModel.privacyDisagree();

        assertFalse(aiPreferences.getAiFeaturesEnabledCurrently());
        assertFalse(entryEditorPreferences.isTabVisible(EntryEditorTabModel.BuiltIn.AI_CHAT));
        assertFalse(entryEditorPreferences.isTabVisible(EntryEditorTabModel.BuiltIn.AI_SUMMARY));
    }

    @Test
    void embeddingModelSizePropertyReflectsConfiguredModelSize() {
        aiPreferences.setEmbeddingModel(PredefinedEmbeddingModel.ZEROHELL_TINYDPR_ACC_0_315_BS_307);

        assertEquals(PredefinedEmbeddingModel.ZEROHELL_TINYDPR_ACC_0_315_BS_307.sizeInfo(),
                viewModel.embeddingModelSizeProperty().get());
    }
}
