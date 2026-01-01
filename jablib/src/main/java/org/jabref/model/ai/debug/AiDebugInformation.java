package org.jabref.model.ai.debug;

import java.util.List;

public class AiDebugInformation {
    public List<AiDebugStep> steps;

    public List<AiDebugStep> getSteps() {
        return steps;
    }

    public void setSteps(List<AiDebugStep> steps) {
        this.steps = steps;
    }
}
