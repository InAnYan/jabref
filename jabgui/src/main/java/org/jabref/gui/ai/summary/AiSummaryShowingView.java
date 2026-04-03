package org.jabref.gui.ai.summary;

import java.time.Instant;
import java.time.ZoneId;
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

import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.WebViewStore;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.summarization.AiSummary;

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

    private static String formatSummaryInfo(AiSummary summary) {
        if (summary == null) {
            return "";
        }

        return Localization.lang("Generated at %0 by %1 (algorithm %2)")
                           .replaceAll("%0", formatTimestamp(summary.timestamp()))
                           .replaceAll("%1", summary.aiProvider().getDisplayName() + " " + summary.model())
                           .replaceAll("%2", summary.summarizationAlgorithm().getDisplayName());
    }

    private static String formatTimestamp(Instant timestamp) {
        return timestamp
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                         .withLocale(Locale.getDefault()));
    }

    public ObjectProperty<AiSummary> summaryProperty() {
        return viewModel.summaryProperty();
    }

    @FXML
    private void initialize() {
        viewModel = new AiSummaryShowingViewModel();
        initializeWebView();

        setupBindings();
        setupListeners();
    }

    private void initializeWebView() {
        webView = WebViewStore.get();
        VBox.setVgrow(webView, Priority.ALWAYS);

        getChildren().addFirst(webView);
    }

    private void setupBindings() {
        viewModel.isMarkdownProperty().bindBidirectional(markdownCheckbox.selectedProperty());

        summaryInfoText.textProperty().bind(BindingsHelper.mapChange(
                viewModel.summaryProperty(),
                AiSummaryShowingView::formatSummaryInfo
        ));
    }

    private void setupListeners() {
        ListenersHelper.onChangeNonNull(
                viewModel.webViewSourceProperty(),
                value -> UiTaskExecutor.runInJavaFXThread(() -> webView.getEngine().loadContent(value))
        );
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
