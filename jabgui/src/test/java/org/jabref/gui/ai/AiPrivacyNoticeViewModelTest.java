package org.jabref.gui.ai;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AiPrivacyNoticeViewModelTest {

    @Test
    void injectorProvidesViewModelWithInjectedPreferences() throws Exception {
        GuiPreferences guiPreferences = mock(GuiPreferences.class);
        DialogService dialogService = mock(DialogService.class);

        Injector.setModelOrService(GuiPreferences.class, guiPreferences);
        Injector.setModelOrService(DialogService.class, dialogService);

        AiPrivacyNoticeViewModel vm = Injector.instantiateModelOrService(AiPrivacyNoticeViewModel.class);

        // use reflection to check that the private field 'preferences' is injected
        var field = AiPrivacyNoticeViewModel.class.getDeclaredField("preferences");
        field.setAccessible(true);
        Object preferencesValue = field.get(vm);

        assertNotNull(preferencesValue, "GuiPreferences should have been injected into AiPrivacyNoticeViewModel");
    }
}

