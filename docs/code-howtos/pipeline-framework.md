---
parent: Code Howtos
---
# Pipeline Framework

The Pipeline Framework provides a structured way to process objects asynchronously through a sequence of transformation stages, with built-in support for progress tracking, caching, and idempotency.

## Overview

The pipeline framework enables developers to create processing workflows that transform objects through multiple stages while providing:

- **Asynchronous Processing**: Uses JabRef's `BackgroundTask` system for non-blocking operations
- **Progress Tracking**: Real-time monitoring of processing state via JavaFX properties
- **Idempotency**: Safe repeated processing of the same objects
- **Result Caching**: Optional caching to avoid redundant computations
- **Type Safety**: Runtime validation of stage compatibility

## Core Concepts

### Pipeline

A pipeline is a sequence of processing stages that transform input objects of type `I` into output objects of type `O`. Each stage processes the result from the previous stage.

### Stages

Individual transformation steps that implement the `PipelineStage<I, O>` interface. Each stage is stateless and performs a specific transformation on the input object.

### Trackers

`PipelineObjectTracker<I, O>` instances that monitor the processing state of individual objects. They provide JavaFX properties for UI binding and state management.

### Caching

`CachedResultsPipeline<I, O>` extends the base pipeline with result caching. If an object has been processed before, it returns the cached result immediately.

### Idempotency

The framework ensures that processing the same object multiple times (by ID) returns the same tracker instance while processing is active, preventing duplicate work.

## Main Classes

### `Pipeline<I, O>`

Base pipeline class that manages a sequence of processing stages with asynchronous execution and runtime type checking between stages.

```java
Pipeline<Document, ProcessedDocument> pipeline = new Pipeline<>(taskExecutor);
pipeline.addStage(new ParseStage());
pipeline.addStage(new ValidateStage());
pipeline.addStage(new TransformStage());

PipelineObjectTracker<Document, ProcessedDocument> tracker = pipeline.process(document);
```

### `CachedResultsPipeline<I, O>`

Extends Pipeline with result caching functionality to avoid redundant computations.

```java
CachedResultsPipeline<Document, ProcessedDocument> cachedPipeline =
    new CachedResultsPipeline<>(taskExecutor, resultsRepository);

PipelineObjectTracker<Document, ProcessedDocument> tracker = cachedPipeline.process(document);
// Returns cached result if document was processed before
```

### `PipelineObjectTracker<I, O>`

Tracks the processing state of an individual object with JavaFX observable properties.

```java
PipelineObjectTracker<Document, ProcessedDocument> tracker = pipeline.process(document);

// Bind to UI properties
progressBar.progressProperty().bind(tracker.stateProperty()
    .map(state -> state == State.FINISHED ? 1.0 : 0.0));

// Check completion
if (tracker.isFinished()) {
    ProcessedDocument result = tracker.getResult().orElse(null);
}
```

**States:**

- `PROCESSING`: Object is currently being processed
- `FINISHED`: Processing completed successfully
- `ERROR`: Processing failed with an exception

### `PipelineStage<I, O>`

Abstract base class for implementing processing stages. Each stage transforms input type `I` to output type `O`.

```java
public class ParseStage extends PipelineStage<String, Document> {
    @Override
    public Document process(String input) {
        // Parse string into document
        return new DocumentParser().parse(input);
    }
}
```

### `ResultsRepository<O>`

Interface for storing and retrieving cached processing results.

```java
public interface ResultsRepository<O extends Identifiable> {
    boolean isProcessed(Identifiable obj);
    Optional<O> getResult(Identifiable obj);
    void storeResult(Identifiable obj, O result);
}
```

### `Identifiable`

Marker interface for objects that can be uniquely identified for caching and idempotency.

```java
public interface Identifiable {
    String getId();
}
```

## Usage Examples

### Basic Pipeline Creation

```java
// Create pipeline with task executor
Pipeline<String, ProcessedData> pipeline = new Pipeline<>(taskExecutor);

// Add processing stages
pipeline.addStage(new ParseStage());
pipeline.addStage(new ValidateStage());
pipeline.addStage(new TransformStage());

// Process an object
String input = "raw data";
PipelineObjectTracker<String, ProcessedData> tracker = pipeline.process(input);

// Monitor progress
tracker.stateProperty().addListener((obs, oldState, newState) -> {
    if (newState == PipelineObjectTracker.State.FINISHED) {
        ProcessedData result = tracker.getResult().get();
        // Handle result
    }
});
```

### Cached Pipeline

```java
// Create cached pipeline for expensive operations
CachedResultsPipeline<Document, AnalysisResult> analysisPipeline =
    new CachedResultsPipeline<>(taskExecutor, new InMemoryResultsRepository());

// First processing - will execute stages
PipelineObjectTracker<Document, AnalysisResult> tracker1 = analysisPipeline.process(document);

// Second processing of same document - returns cached result immediately
PipelineObjectTracker<Document, AnalysisResult> tracker2 = analysisPipeline.process(document);
assert tracker2.isFinished(); // Already cached
```

### Progress Monitoring

```java
PipelineObjectTracker<Document, Result> tracker = pipeline.process(document);

// Bind to UI components
statusLabel.textProperty().bind(tracker.stateProperty()
    .map(State::name));

progressIndicator.visibleProperty().bind(tracker.stateProperty()
    .map(state -> state == State.PROCESSING));

// Handle completion
tracker.stateProperty().addListener((obs, oldState, newState) -> {
    switch (newState) {
        case FINISHED -> handleSuccess(tracker.getResult().get());
        case ERROR -> handleError(tracker.getError().get());
    }
});
```

### Custom Stages

```java
public class DataValidationStage extends PipelineStage<RawData, ValidatedData> {
    @Override
    public ValidatedData process(RawData input) {
        // Perform validation
        if (!isValid(input)) {
            throw new ValidationException("Invalid data format");
        }
        return new ValidatedData(input);
    }
}

public class EnrichmentStage extends PipelineStage<ValidatedData, EnrichedData> {
    @Override
    public EnrichedData process(ValidatedData input) {
        // Add additional data
        return new EnrichedData(input, fetchAdditionalInfo(input.getId()));
    }
}
