package org.jabref.gui.ai.components;

import one.jpro.platform.mdfx.MarkdownView;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.util.markdown.MarkdownToTextFlowParser;
import org.jabref.preferences.WorkspacePreferences;

import java.net.URL;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.text.TextFlow;

public class JabRefMarkdownView extends TextFlow {
    public final StringProperty content;

    private final MarkdownToTextFlowParser markdownToTextFlowParser;

    public JabRefMarkdownView(String content) {
        this.content = new SimpleStringProperty(content);
        this.markdownToTextFlowParser = new MarkdownToTextFlowParser(this);

        updateTextFlow();

        this.content.addListener(obs -> updateTextFlow());
    }

    public String getContent() {
        return content.get();
    }

    public void setContent(String content) {
        this.content.set(content);
    }

    public StringProperty contentProperty() {
        return content;
    }

    private void updateTextFlow() {
        this.getChildren().clear();
        markdownToTextFlowParser.addMarkdown(getContent());
    }
}
