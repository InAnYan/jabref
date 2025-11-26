package org.jabref.logic.ai.current;

import java.util.stream.Stream;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.documentsplitting.DocumentSplitterAlgorithm;
import org.jabref.logic.ai.rag.logic.documentsplitting.SlidingWindowDocumentSplitter;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.rag.DocumentSplittingStrategy;

import org.jspecify.annotations.Nullable;

public class CurrentlySelectedDocumentSplitter implements DocumentSplitterAlgorithm {
    private final AiPreferences aiPreferences;

    @Nullable
    private DocumentSplitterAlgorithm documentSplitterAlgorithm = null;

    public CurrentlySelectedDocumentSplitter(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        update();
        configure();
    }

    private void configure() {
        aiPreferences.customizeExpertSettingsProperty().addListener(_ -> update());
        aiPreferences.documentSplittingStrategyProperty().addListener(_ -> update());
        aiPreferences.documentSplitterChunkSizeProperty().addListener(_ -> update());
        aiPreferences.documentSplitterOverlapSizeProperty().addListener(_ -> update());
    }

    private void update() {
        // Because in the future there will be more strategies.
        //noinspection SwitchStatementWithTooFewBranches
        switch (aiPreferences.getDocumentSplittingStrategy()) {
            case DocumentSplittingStrategy.SLIDING_WINDOW -> {
                documentSplitterAlgorithm = new SlidingWindowDocumentSplitter(
                        aiPreferences.getDocumentSplitterChunkSize(),
                        aiPreferences.getDocumentSplitterOverlapSize()
                );
            }
        }
    }

    @Override
    public Stream<String> split(LongTaskInfo longTaskInfo, String text) throws InterruptedException {
        if (documentSplitterAlgorithm == null) {
            return Stream.of(text);
        }

        return documentSplitterAlgorithm.split(longTaskInfo, text);
    }

    @Override
    public DocumentSplittingStrategy getStrategy() {
        if (documentSplitterAlgorithm == null) {
            // Unfortunately.
            return null;
        }

        return documentSplitterAlgorithm.getStrategy();
    }
}
