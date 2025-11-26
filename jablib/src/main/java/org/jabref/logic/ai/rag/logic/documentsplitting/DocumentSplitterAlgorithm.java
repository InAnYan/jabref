package org.jabref.logic.ai.rag.logic.documentsplitting;

import java.util.stream.Stream;

import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.model.ai.rag.DocumentSplittingStrategy;

public interface DocumentSplitterAlgorithm {
    Stream<String> split(LongTaskInfo longTaskInfo, String text) throws InterruptedException;

    DocumentSplittingStrategy getStrategy();
}
