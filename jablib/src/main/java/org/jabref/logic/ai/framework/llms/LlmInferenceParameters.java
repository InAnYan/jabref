package org.jabref.logic.ai.framework.llms;

/**
 * Parameters for controlling LLM inference behavior.
 */
public class LlmInferenceParameters {
    private final double temperature;

    private LlmInferenceParameters(double temperature) {
        this.temperature = temperature;
    }

    /**
     * Creates a builder for LlmInferenceParameters.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates default inference parameters.
     *
     * @return default parameters with temperature 0.7
     */
    public static LlmInferenceParameters defaultParameters() {
        return builder().temperature(0.7).build();
    }

    /**
     * Returns the temperature parameter.
     *
     * @return the temperature value
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Builder for LlmInferenceParameters.
     */
    public static class Builder {
        private double temperature = 0.7;

        /**
         * Sets the temperature parameter.
         *
         * @param temperature Controls randomness (0.0 = deterministic, 1.0 = very random)
         * @return this builder
         */
        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * Builds the LlmInferenceParameters instance.
         *
         * @return the built parameters
         */
        public LlmInferenceParameters build() {
            return new LlmInferenceParameters(temperature);
        }
    }
}
