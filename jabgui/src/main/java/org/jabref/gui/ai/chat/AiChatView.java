package org.jabref.gui.ai.chat;

import java.util.List;

import javafx.fxml.FXML;

import org.jabref.logic.ai.AiService;
import org.jabref.model.ai.identifiers.FullBibEntryAiIdentifier;

import jakarta.inject.Inject;

public class AiChatView {
    private AiChatViewModel viewModel;

    @Inject
    private AiService aiService;

    @FXML
    private void initialize() {
        viewModel = new AiChatViewModel(aiService);
    }

    public void setEntries(List<FullBibEntryAiIdentifier> entries) {
        this.viewModel.setEntries(entries);
    }
}
