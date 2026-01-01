package org.jabref.model.ai.debug;

public class EmbeddingModelStep extends AiDebugStep {
    public String modelName;
    public int embeddingSize;

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getEmbeddingSize() {
        return embeddingSize;
    }

    public void setEmbeddingSize(int embeddingSize) {
        this.embeddingSize = embeddingSize;
    }
}
