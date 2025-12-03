package org.jabref.gui.ai.summary;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.logic.layout.format.MarkdownFormatter;
import org.jabref.model.ai.summarization.BibEntrySummary;

public class AiSummaryShowingViewModel {
    private static final MarkdownFormatter MARKDOWN_FORMATTER = new MarkdownFormatter();

    private final ObjectProperty<BibEntrySummary> summary = new SimpleObjectProperty<>();
    private final BooleanProperty isMarkdown = new SimpleBooleanProperty(false);

    private final StringProperty webViewSource = new SimpleStringProperty("");

    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerate = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerateCustom = new SimpleObjectProperty<>();

    public AiSummaryShowingViewModel() {
        summary.addListener(_ -> updateWebViewSource());
        isMarkdown.addListener(_ -> updateWebViewSource());
    }

    public void regenerate() {
        if (onRegenerate.get() != null) {
            onRegenerate.get().handle(new ActionEvent());
        }
    }

    public void regenerateCustom() {
        if (onRegenerateCustom.get() != null) {
            onRegenerateCustom.get().handle(new ActionEvent());
        }
    }

    private void updateWebViewSource() {
        if (summary.get() == null) {
            return;
        }

        String content = summary.get().content();
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

    public ObjectProperty<BibEntrySummary> summaryProperty() {
        return summary;
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

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateCustomProperty() {
        return onRegenerateCustom;
    }
}
