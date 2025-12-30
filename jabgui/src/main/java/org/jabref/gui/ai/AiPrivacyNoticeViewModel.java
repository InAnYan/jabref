package org.jabref.gui.ai;

import java.io.IOException;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiPrivacyNoticeViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiPrivacyNoticeViewModel.class);

    private final StringProperty embeddingModelSize = new SimpleStringProperty("");

    private final StringProperty privacyDisagreeButtonText = new SimpleStringProperty("");
    private final ObjectProperty<EventHandler<ActionEvent>> onPrivacyDisagree = new SimpleObjectProperty<>();

    private final GuiPreferences preferences;
    private final DialogService dialogService;

    public AiPrivacyNoticeViewModel(
            GuiPreferences guiPreferences,
            DialogService dialogService
    ) {
        this.preferences = guiPreferences;
        this.dialogService = dialogService;

        preferences.getAiPreferences().embeddingModelProperty().addListener((_, _, value) ->
                embeddingModelSize.set(value.sizeInfo())
        );
    }

    public StringProperty embeddingModelSizeProperty() {
        return embeddingModelSize;
    }

    public void onPrivacyAgree() {
        preferences.getAiPreferences().setEnableAi(true);
    }

    public void openBrowser(String link) {
        try {
            NativeDesktop.openBrowser(link, preferences.getExternalApplicationsPreferences());
        } catch (IOException e) {
            LOGGER.error("Error opening the browser to the Privacy Policy page of the AI provider.", e);

            dialogService.showErrorDialogAndWait(e);
        }
    }

    public void privacyDisagree() {
        if (onPrivacyDisagree.get() != null) {
            onPrivacyDisagree.get().handle(new ActionEvent());
        }
    }

    public ObjectProperty<EventHandler<ActionEvent>> onPrivacyDisagreeProperty() {
        return onPrivacyDisagree;
    }

    public StringProperty privacyDisagreeButtonTextProperty() {
        return privacyDisagreeButtonText;
    }
}
