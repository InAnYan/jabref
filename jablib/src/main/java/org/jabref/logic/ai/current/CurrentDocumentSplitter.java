package org.jabref.logic.ai.current;

import java.util.stream.Stream;

import org.jabref.logic.ai.pipeline.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.pipeline.logic.documentsplitting.SlidingWindowDocumentSplitter;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;

import org.jspecify.annotations.Nullable;

public class CurrentDocumentSplitter implements DocumentSplitter {
    private final AiPreferences aiPreferences;

    @Nullable
    private DocumentSplitter documentSplitter = null;

    public CurrentDocumentSplitter(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        update();
        configure();
    }

    private void configure() {
        aiPreferences.customizeExpertSettingsProperty().addListener(_ -> update());
        aiPreferences.documentSplitterKindProperty().addListener(_ -> update());
        aiPreferences.documentSplitterChunkSizeProperty().addListener(_ -> update());
        aiPreferences.documentSplitterOverlapSizeProperty().addListener(_ -> update());
    }

    private void update() {
        // Because in the future there will be more strategies.
        //noinspection SwitchStatementWithTooFewBranches
        switch (aiPreferences.getDocumentSplitterKind()) {
            case DocumentSplitterKind.SLIDING_WINDOW -> {
                documentSplitter = new SlidingWindowDocumentSplitter(
                        aiPreferences.getDocumentSplitterChunkSize(),
                        aiPreferences.getDocumentSplitterOverlapSize()
                );
            }
        }
    }

    @Override
    public Stream<String> split(LongTaskInfo longTaskInfo, String text) throws InterruptedException {
        if (documentSplitter == null) {
            return Stream.of(text);
        }

        return documentSplitter.split(longTaskInfo, text);
    }

    @Override
    public DocumentSplitterKind getKind() {
        aiPreferences.getDocumentSplitterKind();
    }
}
