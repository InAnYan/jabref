package org.jabref.gui.ai.summary;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.PropertiesHelper;
import org.jabref.logic.layout.format.MarkdownFormatter;
import org.jabref.model.ai.summarization.AiSummary;

public class AiSummaryShowingViewModel extends AbstractViewModel {
    private static final MarkdownFormatter MARKDOWN_FORMATTER = new MarkdownFormatter();

    private final ObjectProperty<AiSummary> summary = new SimpleObjectProperty<>();
    private final BooleanProperty isMarkdown = new SimpleBooleanProperty(true);

    private final StringProperty webViewSource = new SimpleStringProperty("");

    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerate = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerateCustom = new SimpleObjectProperty<>();

    public AiSummaryShowingViewModel() {
        setupBindings();
    }

    private static String generateWebSource(AiSummary summary, Boolean isMarkdown) {
        if (summary == null || isMarkdown == null) {
            return "";
        }

        String content = summary.content();

        if (isMarkdown) {
            return MARKDOWN_FORMATTER.format(content);
        } else {
            return "<body style='margin: 0; padding: 5px; width: 100vw'>" +
                    "<div style='white-space: pre-wrap; word-wrap: break-word; width: 100vw'>" +
                    content +
                    "</div></body>";
        }
    }

    private void setupBindings() {
        webViewSource.bind(BindingsHelper.map(
                summary, isMarkdown,
                AiSummaryShowingViewModel::generateWebSource
        ));
    }

    public void regenerate() {
        PropertiesHelper.handle(onRegenerate);
    }

    public void regenerateCustom() {
        PropertiesHelper.handle(onRegenerateCustom);
    }

    public ObjectProperty<AiSummary> summaryProperty() {
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
