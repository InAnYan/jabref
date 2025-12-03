package org.jabref.gui.ai.summary;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.l10n.Localization;

public class AiSummaryParametersDialog extends BaseDialog<Summarizator> {
    private final AiSummaryParametersView aiSummaryParametersView;

    public AiSummaryParametersDialog() {
        super();
        this.setTitle(Localization.lang("Summarization parameters"));
        this.aiSummaryParametersView = new AiSummaryParametersView();
        this.setResultConverter(_ -> aiSummaryParametersView.constructSummarizator());
    }

    public Summarizator constructSummarizator() {
        return aiSummaryParametersView.constructSummarizator();
    }
}
