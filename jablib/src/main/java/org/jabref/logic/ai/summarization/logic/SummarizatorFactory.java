package org.jabref.logic.ai.summarization.logic;

import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.ChunkedSummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.FullDocumentSummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.ai.summarization.SummarizatorKind;

public class SummarizatorFactory {
    private SummarizatorFactory() {
    }

    public static Summarizator create(
            SummarizatorKind summarizatorKind,
            String summarizationChunkSystemMessageTemplate,
            String summarizationCombineSystemMessageTemplate,
            String summarizationFullDocumentSystemMessageTemplate
    ) {
        return switch (summarizatorKind) {
            case SummarizatorKind.CHUNKED ->
                    new ChunkedSummarizator(
                            summarizationChunkSystemMessageTemplate,
                            summarizationCombineSystemMessageTemplate
                    );

            case SummarizatorKind.FULL_DOCUMENT ->
                    new FullDocumentSummarizator(
                            summarizationFullDocumentSystemMessageTemplate
                    );
        };
    }
}
