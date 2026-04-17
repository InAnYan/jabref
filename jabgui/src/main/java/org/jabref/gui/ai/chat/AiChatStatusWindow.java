package org.jabref.gui.ai.chat;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.airhacks.afterburner.views.ViewLoader;

/// Dialog that shows the AI chat status (ingestion, answer engine, model config).
///
/// Returns the configured {@link ChatModel} when the user clicks Apply,
/// or {@code null} when the user clicks Cancel / closes the window.
public class AiChatStatusWindow extends BaseDialog<ChatModel> {
    @FXML private AiChatStatusView aiChatStatusView;

    public AiChatStatusWindow() {
        super();

        this.setTitle(Localization.lang("AI chat status"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        setResultConverter(buttonType -> {
            if (buttonType != null && buttonType.getButtonData() == ButtonBar.ButtonData.APPLY) {
                // Return the current value from the reactive binding.
                return aiChatStatusView.chatModelProperty().getValue();
            }
            return null;
        });
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return aiChatStatusView.chatHistoryProperty();
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return aiChatStatusView.answerEngineProperty();
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return aiChatStatusView.entriesProperty();
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return aiChatStatusView.generateEmbeddingsTasksProperty();
    }

    /// Returns a reactive binding that always reflects the current {@link ChatModel}
    /// from the chat config panel. Callers may observe this live while the dialog is open.
    public ObservableValue<ChatModel> chatModelProperty() {
        return aiChatStatusView.chatModelProperty();
    }

    /// Returns a reactive binding that always reflects the current {@link Summarizator}
    /// from the summarization config panel.
    public ObservableValue<Summarizator> summarizatorProperty() {
        return aiChatStatusView.summarizatorProperty();
    }
}
