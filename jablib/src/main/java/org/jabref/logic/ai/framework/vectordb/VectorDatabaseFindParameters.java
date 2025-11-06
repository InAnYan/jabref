package org.jabref.logic.ai.framework.vectordb;

/**
 * Parameters for vector database similarity search operations.
 */
public class VectorDatabaseFindParameters {
    private final double minimumScore;
    private final int maximumDocumentsCount;

    private VectorDatabaseFindParameters(double minimumScore, int maximumDocumentsCount) {
        if (minimumScore <= 0) {
            throw new IllegalArgumentException("minimumScore must be positive");
        }
        if (maximumDocumentsCount <= 0) {
            throw new IllegalArgumentException("maximumDocumentsCount must be positive");
        }
        this.minimumScore = minimumScore;
        this.maximumDocumentsCount = maximumDocumentsCount;
    }

    /**
     * Creates a builder for VectorDatabaseFindParameters.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates default find parameters.
     *
     * @return default parameters with minimumScore 0.7 and maximumDocumentsCount 10
     */
    public static VectorDatabaseFindParameters defaultParameters() {
        return builder().minimumScore(0.7).maximumDocumentsCount(10).build();
    }

    /**
     * Returns the minimum similarity score for results to be included.
     *
     * @return the minimum score
     */
    public double getMinimumScore() {
        return minimumScore;
    }

    /**
     * Returns the maximum number of documents to return.
     *
     * @return the maximum count
     */
    public int getMaximumDocumentsCount() {
        return maximumDocumentsCount;
    }

    /**
     * Builder for VectorDatabaseFindParameters.
     */
    public static class Builder {
        private double minimumScore = 0.7;
        private int maximumDocumentsCount = 10;

        /**
         * Sets the minimum similarity score for results to be included.
         *
         * @param minimumScore minimum similarity score (must be positive)
         * @return this builder
         */
        public Builder minimumScore(double minimumScore) {
            this.minimumScore = minimumScore;
            return this;
        }

        /**
         * Sets the maximum number of documents to return.
         *
         * @param maximumDocumentsCount maximum number of documents (must be positive)
         * @return this builder
         */
        public Builder maximumDocumentsCount(int maximumDocumentsCount) {
            this.maximumDocumentsCount = maximumDocumentsCount;
            return this;
        }

        /**
         * Builds the VectorDatabaseFindParameters instance.
         *
         * @return the built parameters
         */
        public VectorDatabaseFindParameters build() {
            return new VectorDatabaseFindParameters(minimumScore, maximumDocumentsCount);
        }
    }
}
