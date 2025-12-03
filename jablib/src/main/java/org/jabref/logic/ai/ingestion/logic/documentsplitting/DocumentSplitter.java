package org.jabref.logic.ai.ingestion.logic.documentsplitting;

import java.util.stream.Stream;

import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;

public interface DocumentSplitter {
    Stream<String> split(LongTaskInfo longTaskInfo, String text) throws InterruptedException;

    DocumentSplitterKind getKind();
}
