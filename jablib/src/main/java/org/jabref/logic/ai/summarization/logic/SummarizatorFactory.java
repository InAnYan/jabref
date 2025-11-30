package org.jabref.logic.ai.summarization.logic;

import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.ChunkedSummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.FullDocumentSummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.templates.AiTemplatesFactory;
import org.jabref.model.ai.summarization.SummarizatorKind;

public class SummarizatorFactory {
    public static Summarizator createSummarizator(
            AiTemplatesFactory templatesFactory,
            SummarizatorKind summarizatorKind
    ) {
        return switch (summarizatorKind) {
            case SummarizatorKind.CHUNKED ->
                    createChunkedSummarizationAlgorithm(templatesFactory);

            case SummarizatorKind.FULL_DOCUMENT ->
                    createFullDocumentSummarizationAlgorithm(templatesFactory);
        };
    }

    private static ChunkedSummarizator createChunkedSummarizationAlgorithm(
            AiTemplatesFactory templatesFactory
    ) {
        return new ChunkedSummarizator(
                templatesFactory.getSummarizationChunkSystemMessageTemplate(),
                templatesFactory.getSummarizationChunkUserMessageTemplate(),
                templatesFactory.getSummarizationCombineSystemMessageTemplate(),
                templatesFactory.getSummarizationCombineUserMessageTemplate()
        );
    }

    private static FullDocumentSummarizator createFullDocumentSummarizationAlgorithm(
            AiTemplatesFactory templatesFactory
    ) {
        return new FullDocumentSummarizator(
                templatesFactory.getSummarizationFullDocumentSystemMessageTemplate(),
                templatesFactory.getSummarizationFullDocumentUserMessageTemplate()
        );
    }
}
