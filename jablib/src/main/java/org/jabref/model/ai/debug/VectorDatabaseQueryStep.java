package org.jabref.model.ai.debug;

import org.jabref.model.ai.embeddings.EmbeddingSimilarityMetric;

public class VectorDatabaseQueryStep extends AiDebugStep {
    public EmbeddingSimilarityMetric similarityMetric;

    public EmbeddingSimilarityMetric getSimilarityMetric() {
        return similarityMetric;
    }

    public void setSimilarityMetric(EmbeddingSimilarityMetric similarityMetric) {
        this.similarityMetric = similarityMetric;
    }
}
