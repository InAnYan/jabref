package org.jabref.gui.entryeditor.aisummary;

import java.time.LocalDateTime;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.logic.layout.format.MarkdownFormatter;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.BibEntrySummary;

public class AiSummaryShowingViewModel {
    private static final MarkdownFormatter MARKDOWN_FORMATTER = new MarkdownFormatter();

    private final ObjectProperty<BibEntrySummary> bibEntrySummary = new SimpleObjectProperty<>();
    private final ObjectProperty<AiProvider> summaryAiProvider = new SimpleObjectProperty<>();
    private final StringProperty summaryModel = new SimpleStringProperty("");
    private final ObjectProperty<LocalDateTime> summaryTimestamp = new SimpleObjectProperty<>();

    private final BooleanProperty isMarkdown = new SimpleBooleanProperty(false);

    private final StringProperty webViewSource = new SimpleStringProperty("");

    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerate = new SimpleObjectProperty<>();

    public AiSummaryShowingViewModel() {
        bibEntrySummary.addListener(_ -> updateSummary());
        bibEntrySummary.addListener(_ -> updateWebViewSource());
        isMarkdown.addListener(_ -> updateWebViewSource());
    }

    public void regenerate() {
        if (onRegenerate.get() != null) {
            onRegenerate.get().handle(new ActionEvent());
        }
    }

    private void updateSummary() {
        summaryAiProvider.set(bibEntrySummary.get().aiProvider());
        summaryModel.set(bibEntrySummary.get().model());
        summaryTimestamp.set(bibEntrySummary.get().timestamp());
    }

    private void updateWebViewSource() {
        String content = bibEntrySummary.get().content();
        if (isMarkdown.get()) {
            webViewSource.set(MARKDOWN_FORMATTER.format(content));
        } else {
            webViewSource.set(
                    "<body style='margin: 0; padding: 5px; width: 100vw'>" +
                            "<div style='white-space: pre-wrap; word-wrap: break-word; width: 100vw'>" +
                            content +
                            "</div></body>"
            );
        }
    }

    public ObjectProperty<BibEntrySummary> bibEntrySummaryProperty() {
        return bibEntrySummary;
    }

    public ObjectProperty<AiProvider> summaryAiProviderProperty() {
        return summaryAiProvider;
    }

    public StringProperty summaryModelProperty() {
        return summaryModel;
    }

    public ObjectProperty<LocalDateTime> summaryTimestampProperty() {
        return summaryTimestamp;
    }

    public BooleanProperty isMarkdownProperty() {
        return isMarkdown;
    }

    public StringProperty webViewSourceProperty() {
        return webViewSource;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return onRegenerate;
    }
}
