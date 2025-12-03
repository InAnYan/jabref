package org.jabref.gui.ai.components.newaichat;

import javafx.fxml.FXML;

public class AiChatView {
    private AiChatViewModel viewModel;

    @FXML
    private void initialize() {
        viewModel = new AiChatViewModel();
    }
}
