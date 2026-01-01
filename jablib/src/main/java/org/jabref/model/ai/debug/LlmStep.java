package org.jabref.model.ai.debug;

import org.jabref.model.ai.llm.AiProvider;

public class LlmStep {
    public AiProvider aiProvider;
    public String modelName;

    public AiProvider getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
