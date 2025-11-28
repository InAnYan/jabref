package org.jabref.logic.ai.preferences;

import org.jabref.model.ai.embeddings.EmbeddingModelEnumeration;
import org.jabref.model.ai.rag.DocumentSplitterKind;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

/// A collection of values for the default settings of AI in the expert section.
///
/// This collection was made because "Expert settings" in the AI settings is resettable.
/// There are facilities in JabRef codebase to reset either all settings or 1 section, but not a part of a section.
public class AiDefaultExpertSettings {
    public static final EmbeddingModelEnumeration EMBEDDING_MODEL = EmbeddingModelEnumeration.SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V2;
    public static final SummarizatorKind SUMMARIZATION_ALGORITHM_NAME = SummarizatorKind.CHUNKED;
    public static final TokenEstimatorKind TOKEN_ESTIMATION_STRATEGY = TokenEstimatorKind.MAX;
    public static final float TEMPERATURE = 0.7f;
    public static final int CONTEXT_WINDOW_SIZE = 8192;
    public static final DocumentSplitterKind DOCUMENT_SPLITTING_STRATEGY = DocumentSplitterKind.SLIDING_WINDOW;
    public static final int DOCUMENT_SPLITTER_CHUNK_SIZE = 300;
    public static final int DOCUMENT_SPLITTER_OVERLAP_SIZE = 100;
    public static final int RAG_MAX_RESULTS_COUNT = 10;
    public static final float RAG_MIN_SCORE = 0.3f;
}
