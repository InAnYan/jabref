package org.jabref.gui.entryeditor.aisummary;

import java.time.LocalDateTime;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.ai.llm.AiProvider;

public class AiSummaryViewModel extends AbstractViewModel {
    public enum State {
        PROCESSING,
        DONE,
        ERROR
    }

    // Global properties.
    private final BooleanProperty showAiPrivacyPolicyGuard = new SimpleBooleanProperty(true);
    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.PROCESSING);

    // Error state properties.
    private final OptionalObjectProperty<Exception> error = new OptionalObjectProperty<>(Optional.empty());

    // Processing state properties.
    private final ObjectProperty<AiProvider> processingAiProvider = new SimpleObjectProperty<>();
    private final StringProperty processingLlmName = new SimpleStringProperty("");

    // Done state properties.
    private final StringProperty summary = new SimpleStringProperty("");
    private final BooleanProperty summaryRenderMarkdown = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDateTime> summaryTimestamp = new SimpleObjectProperty<>();
    private final ObjectProperty<AiProvider> summaryAiProvider = new SimpleObjectProperty<>();
    private final StringProperty summaryLlmName = new SimpleStringProperty("");

    public AiSummaryViewModel(
            AiPreferences aiPreferences
    ) {
        showAiPrivacyPolicyGuard.bind(aiPreferences.enableAiProperty());
        aiPreferences.aiProviderProperty().bind(processingAiProvider);
        aiPreferences.addListenerToChatModels(() -> processingLlmName.set(aiPreferences.getSelectedChatModel()));

        startSummarization();
    }

    public void regenerate() {
        clearSummary();
        startSummarization();
    }

    private void clearSummary() {

    }

    public BooleanProperty showAiPrivacyPolicyGuardProperty() {
        return showAiPrivacyPolicyGuard;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public OptionalObjectProperty<Exception> errorProperty() {
        return error;
    }

    public ObjectProperty<AiProvider> processingAiProviderProperty() {
        return processingAiProvider;
    }

    public StringProperty processingLlmNameProperty() {
        return processingLlmName;
    }

    public StringProperty summaryProperty() {
        return summary;
    }

    public BooleanProperty summaryRenderMarkdownProperty() {
        return summaryRenderMarkdown;
    }

    public ObjectProperty<LocalDateTime> summaryTimestampProperty() {
        return summaryTimestamp;
    }

    public ObjectProperty<AiProvider> summaryAiProviderProperty() {
        return summaryAiProvider;
    }

    public StringProperty summaryLlmNameProperty() {
        return summaryLlmName;
    }
}
