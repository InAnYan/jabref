package org.jabref.gui.ai.summary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;

import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.WebViewStore;
import org.jabref.model.ai.summarization.BibEntrySummary;

import com.airhacks.afterburner.views.ViewLoader;

public class AiSummaryShowingView extends VBox {
    @FXML private CheckBox markdownCheckbox;
    @FXML private Text summaryInfoText;

    private WebView webView;

    private AiSummaryShowingViewModel viewModel;

    public AiSummaryShowingView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public ObjectProperty<BibEntrySummary> summaryProperty() {
        return viewModel.summaryProperty();
    }

    @FXML
    private void initialize() {
        viewModel = new AiSummaryShowingViewModel();
        initializeWebView();

        viewModel.isMarkdownProperty().bindBidirectional(markdownCheckbox.selectedProperty());
        viewModel.webViewSourceProperty().addListener((_, _, value) ->
                UiTaskExecutor.runInJavaFXThread(() -> webView.getEngine().loadContent(value)));
        viewModel.summaryProperty().addListener(_ -> updateSummaryInfo());
    }

    private void initializeWebView() {
        webView = WebViewStore.get();
        VBox.setVgrow(webView, Priority.ALWAYS);

        getChildren().addFirst(webView);
    }

    private void updateSummaryInfo() {
        if (viewModel.summaryProperty().get() == null) {
            return;
        }

        String newInfo = summaryInfoText
                .getText()
                .replaceAll("%0", formatTimestamp(viewModel.summaryProperty().get().timestamp()))
                .replaceAll("%1", viewModel.summaryProperty().get().aiProvider().getDisplayName() + " " + viewModel.summaryProperty().get().model())
                .replaceAll("%2", viewModel.summaryProperty().get().summarizationAlgorithm().getDisplayName());
        summaryInfoText.setText(newInfo);
    }

    private static String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()));
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return viewModel.onRegenerateProperty();
    }

    public EventHandler<ActionEvent> getOnRegenerate() {
        return viewModel.onRegenerateProperty().get();
    }

    public void setOnRegenerate(EventHandler<ActionEvent> onRegenerate) {
        viewModel.onRegenerateProperty().set(onRegenerate);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateCustomProperty() {
        return viewModel.onRegenerateCustomProperty();
    }

    public EventHandler<ActionEvent> getOnRegenerateCustom() {
        return viewModel.onRegenerateCustomProperty().get();
    }

    public void setOnRegenerateCustom(EventHandler<ActionEvent> onRegenerateCustom) {
        viewModel.onRegenerateCustomProperty().set(onRegenerateCustom);
    }

    @FXML
    private void regenerate() {
        viewModel.regenerate();
    }

    @FXML
    private void regenerateCustom() {
        viewModel.regenerateCustom();
    }
}
