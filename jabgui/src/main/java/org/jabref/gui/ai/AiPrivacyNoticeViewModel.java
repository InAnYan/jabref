package org.jabref.gui.ai;

import java.io.IOException;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiPrivacyNoticeViewModel extends AbstractViewModel {
    public enum DisagreeBehaviour {
        HIDE_AI_TABS,
        HIDE_CHAT_WITH_GROUP_BUTTON;

        public String toLocalizedString() {
            return switch (this) {
                case HIDE_AI_TABS ->
                        Localization.lang("Hide 'AI' tabs");
                case HIDE_CHAT_WITH_GROUP_BUTTON ->
                        Localization.lang("Hide 'Chat with group' button");
            };
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AiPrivacyNoticeViewModel.class);

    private final StringProperty embeddingModelSize = new SimpleStringProperty("");

    private final ObjectProperty<DisagreeBehaviour> disagreeBehaviour = new SimpleObjectProperty<>(DisagreeBehaviour.HIDE_AI_TABS);
    private final StringProperty privacyDisagreeButtonText = new SimpleStringProperty("");

    private final AiPreferences aiPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final GroupsPreferences groupsPreferences;
    private final DialogService dialogService;

    public AiPrivacyNoticeViewModel(
            AiPreferences aiPreferences,
            ExternalApplicationsPreferences externalApplicationsPreferences,
            EntryEditorPreferences entryEditorPreferences,
            GroupsPreferences groupsPreferences,
            DialogService dialogService
    ) {
        this.aiPreferences = aiPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.entryEditorPreferences = entryEditorPreferences;
        this.groupsPreferences = groupsPreferences;
        this.dialogService = dialogService;

        setupBindings();
    }

    private void setupBindings() {
        privacyDisagreeButtonText.bind(disagreeBehaviour.map(DisagreeBehaviour::toLocalizedString));
        embeddingModelSize.bind(aiPreferences.embeddingModelProperty().map(PredefinedEmbeddingModel::sizeInfo));
    }

    public void onPrivacyAgree() {
        aiPreferences.setEnableAi(true);
    }

    public void openBrowser(String link) {
        try {
            NativeDesktop.openBrowser(link, externalApplicationsPreferences);
        } catch (IOException e) {
            LOGGER.error("Error opening the browser to the Privacy Policy page of the AI provider.", e);
            dialogService.showErrorDialogAndWait(e);
        }
    }

    public void privacyDisagree() {
        switch (disagreeBehaviour.get()) {
            case HIDE_AI_TABS -> {
                entryEditorPreferences.setShouldShowAiChatTab(false);
                entryEditorPreferences.setShouldShowAiSummaryTab(false);
            }
            case HIDE_CHAT_WITH_GROUP_BUTTON -> {
                groupsPreferences.setShowAiChatButton(false);
            }
        }
    }

    public ReadOnlyStringProperty embeddingModelSizeProperty() {
        return embeddingModelSize;
    }

    public ReadOnlyStringProperty privacyDisagreeButtonTextProperty() {
        return privacyDisagreeButtonText;
    }

    public ObjectProperty<DisagreeBehaviour> disagreeBehaviourProperty() {
        return disagreeBehaviour;
    }
}
