package org.jabref.logic.ai.summarization.logic;

import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.ChunkedSummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.FullDocumentSummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.templates.AiTemplatesFactory;
import org.jabref.model.ai.summarization.SummarizatorKind;

public class SummarizatorFactory {
    private final AiTemplatesFactory templatesFactory;

    public SummarizatorFactory(AiTemplatesFactory templatesFactory) {
        this.templatesFactory = templatesFactory;
    }

    public Summarizator create(
            SummarizatorKind summarizatorKind
    ) {
        return switch (summarizatorKind) {
            case SummarizatorKind.CHUNKED ->
                    new ChunkedSummarizator(
                            templatesFactory.getSummarizationChunkSystemMessageTemplate(),
                            templatesFactory.getSummarizationChunkUserMessageTemplate(),
                            templatesFactory.getSummarizationCombineSystemMessageTemplate(),
                            templatesFactory.getSummarizationCombineUserMessageTemplate()
                    );

            case SummarizatorKind.FULL_DOCUMENT ->
                    new FullDocumentSummarizator(
                            templatesFactory.getSummarizationFullDocumentSystemMessageTemplate(),
                            templatesFactory.getSummarizationFullDocumentUserMessageTemplate()
                    );
        };
    }
}
