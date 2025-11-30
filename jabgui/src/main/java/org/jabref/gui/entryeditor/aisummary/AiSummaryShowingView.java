package org.jabref.gui.entryeditor.aisummary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;

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

    public ObjectProperty<BibEntrySummary> bibEntrySummaryProperty() {
        return viewModel.bibEntrySummaryProperty();
    }

    @FXML
    private void initialize() {
        viewModel = new AiSummaryShowingViewModel();
        initializeWebView();

        viewModel.isMarkdownProperty().bindBidirectional(markdownCheckbox.selectedProperty());
        viewModel.webViewSourceProperty().addListener((_, _, value) ->
                webView.getEngine().loadContent(value));
        viewModel.summaryModelProperty().addListener(_ -> updateSummaryInfo());
        viewModel.summaryTimestampProperty().addListener(_ -> updateSummaryInfo());
        viewModel.summaryAiProviderProperty().addListener(_ -> updateSummaryInfo());
    }

    private void initializeWebView() {
        webView = WebViewStore.get();
        VBox.setVgrow(webView, Priority.ALWAYS);

        getChildren().addFirst(webView);
    }

    private void updateSummaryInfo() {
        String newInfo = summaryInfoText
                .getText()
                .replaceAll("%0", formatTimestamp(viewModel.summaryTimestampProperty().get()))
                .replaceAll("%1", viewModel.summaryAiProviderProperty().get().getDisplayName() + " " + viewModel.summaryModelProperty().get());
        summaryInfoText.setText(newInfo);
    }

    private static String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()));
    }
}
