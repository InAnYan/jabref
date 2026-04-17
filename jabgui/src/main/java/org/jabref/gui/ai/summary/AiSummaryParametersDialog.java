package org.jabref.gui.ai.summary;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class AiSummaryParametersDialog extends BaseDialog<AiSummaryParametersDialog.SummaryCustomConfig> {

    /// Holds both the custom summarizator algorithm and the chat model to use.
    public record SummaryCustomConfig(Summarizator summarizator, ChatModel chatModel) {
    }

    @FXML private AiSummaryParametersView aiSummaryParametersView;

    public AiSummaryParametersDialog() {
        super();
        this.setTitle(Localization.lang("Summarization parameters"));
        this.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return null;
            } else {
                return new SummaryCustomConfig(
                        aiSummaryParametersView.summarizatorProperty().getValue(),
                        aiSummaryParametersView.chatModelProperty().getValue()
                );
            }
        });

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }
}
