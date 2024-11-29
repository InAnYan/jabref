package org.jabref.gui.ai.components.summary;

import java.time.LocalDateTime; import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
import org.jabref.logic.ai.summarization.Summary;

import com.airhacks.afterburner.views.ViewLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryShowingComponent extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryShowingComponent.class);

    private final AiPreferences aiPreferences;
    private final AiService aiService;
    private final BibEntry entry;
    private final BibDatabaseContext bibDatabaseContext;

    @FXML private Slider slider;
    @FXML private Button generateButton;
    @FXML private Text estimatedPointsText;

    private boolean hasSummary = false;

    public SummaryShowingComponent(
            AiPreferences aiPreferences,
            AiService aiService,
            BibEntry entry,
            BibDatabaseContext bibDatabaseContext
    ) {
        this.aiPreferences = aiPreferences;
        this.aiService = aiService;
        this.entry = entry;
        this.bibDatabaseContext = bibDatabaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();

        setStateNoSummary();
    }

    @FXML
    private void generateSummary() {
        setStateGeneratingSummary();

        ProcessingInfo<BibEntry, Summary> processingInfo = hasSummary
                ? aiService.getSummariesService().regenerateSummary(entry, slider.getValue(), bibDatabaseContext)
                : aiService.getSummariesService().summarize(entry, slider.getValue(), bibDatabaseContext);

        processingInfo.stateProperty().addListener((observable, oldValue, newValue) -> {
            hasSummary = newValue == ProcessingState.SUCCESS;
            generateButton.setCancelButton(newValue == ProcessingState.PROCESSING);

            switch (newValue) {
                case SUCCESS -> {
                    assert processingInfo.getData().isPresent(); // When the state is SUCCESS, the data must be present.
                    setStateSummary(processingInfo.getData().get());
                }
                case ERROR -> setStateError(processingInfo);
                case PROCESSING -> setStateGeneratingSummary();
                case STOPPED -> setStateNoSummary();
            }
        });
    }

    private void setStateNoSummary() {
        setState(
                new ErrorStateComponent(
                        Localization.lang("No summary"),
                        Localization.lang("Click on 'Generate' button to generate a new summary")
                )
        );
    }

    private void setStateGeneratingSummary() {
        setState(
                ErrorStateComponent.withSpinner(
                        Localization.lang("Processing..."),
                        Localization.lang("The attached file(s) are currently being processed by %0. Once completed, you will be able to see the summary.", aiPreferences.getAiProvider().getLabel())
                )
        );
    }

    private void setStateSummary(Summary summary) {
        TextArea textArea = new TextArea(summary.content());
        textArea.setWrapText(true);

        Text info = new Text(Localization.lang(
                "Generated at %0 by %1",
                formatTimestamp(summary.timestamp()),
                summary.aiProvider().getLabel() + " " + summary.model()
        ));
        AnchorPane anchorPane = new AnchorPane(info);

        VBox vBox = new VBox(10, textArea, anchorPane);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        setState(vBox);
    }

    private void setStateError(ProcessingInfo<BibEntry, Summary> processingInfo) {
        assert processingInfo.getException().isPresent(); // When the state is ERROR, the exception must be present.

        LOGGER.error("Got an error while generating a summary for entry {}", entry.getCitationKey().orElse("<no citation key>"), processingInfo.getException().get());

        setState(ErrorStateComponent.withTextArea(
                Localization.lang("Unable to chat"),
                Localization.lang("Got error while processing the file:"),
                processingInfo.getException().get().getLocalizedMessage()
        ));
    }

    private static String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.format(
                DateTimeFormatter
                        .ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault())
        );
    }

    private void setState(Node node) {
        if (getChildren().size() > 1) {
            getChildren().removeLast();
        }

        getChildren().add(node);
    }
}
