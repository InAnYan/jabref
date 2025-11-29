package org.jabref.gui.entryeditor.aisummary;

import javafx.scene.layout.Pane;

import com.airhacks.afterburner.views.ViewLoader;

public class AiSummaryView extends Pane {
    public AiSummaryView() {
        ViewLoader
                .view(this)
                .root(this)
                .load();
    }
}
