package org.jabref.logic.ai.pipeline.logic.documentsplitting;

import java.util.stream.Stream;

import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;

public interface DocumentSplitter {
    Stream<String> split(LongTaskInfo longTaskInfo, String text) throws InterruptedException;

    DocumentSplitterKind getKind();
}
