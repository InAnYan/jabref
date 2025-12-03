package org.jabref.gui.ai.components.newaichat;

import javafx.fxml.FXML;

import org.jabref.logic.ai.AiService;

import jakarta.inject.Inject;

public class AiChatView {
    private AiChatViewModel viewModel;

    @Inject
    private AiService aiService;

    @FXML
    private void initialize() {
        viewModel = new AiChatViewModel(aiService);
    }
}
