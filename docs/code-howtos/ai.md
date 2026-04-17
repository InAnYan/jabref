---
parent: Code Howtos
---

# AI

The JabRef has next AI features:

- Chatting with entries,
- Chatting with groups,
- Summarization of entries,
- Parsing of plain citations using LLMs
- Extracting "References" section from PDFs with the help of LLMs.

The initial AI features were made in the [GSoC 2024 Project "AI-Powered Summarization and “Interaction” with Academic Papers"](https://github.com/InAnYan/gsoc).

The features are built on [LangChain4j](https://github.com/langchain4j/langchain4j) and [Deep Java Library](https://djl.ai/).

The RAG (Retrieval Augmented Generation) is explained in [ADR 0037](./../0037-rag-architecture-implementation.md).

## Features

### Chatting with entries

### Chatting with groups

### Summarization of entries

### Parsing of plain citations

### Extracting "References" section

## Code organization

As every JabRef feature, AI is divided into 3 layers: GUI, logic, and model. Inside the `logic` package the AI code is split by feature (each feature has its own package).

The GUI code strongly follows [MVVM pattern](./javafx.md). Though, the GUI code is a bit complicated as:

1. Most of the core GUI components (chat and summary components) are designed as a state machine. Typical states include: loading, presenting the result, error, etc.
2. These core GUI components are also made that way so it would be possible to rebind them to another `BibEntry`. This feature is highly important, but its implementation is complex. For the details, take a look at the section [How to add a new AI feature](## How to add a new AI feature).

## Internal model (v2)

There are 3 core models in the AI features:

1. Chat history.
2. Summaries.
3. Embeddings.
4. Fully ingested documents.

The code strictly follows the repository idiom, where an interface is created to access the internal storage for the purpose of abstraction. At the moment of writing, all of these models are implemented by using the [`MVStore`](https://www.h2database.com/html/mvstore.html). For the details of this decisions take a look at the [ADR 0033](./../0033-store-chats-in-mvstore.md). A helper class was made `MVStoreBase` so that it would be possible to use an in-memory `MVStore` in case there are some errors while opening on-disk storage.

A note needs to be made for embeddings: the embeddings storage is also implementing the internal LangChain4j interface for embeddings so that it could be used in LangChain4j algorithms. Additionally, there is a "fully ingested" repository, which simply contains a "list" of files that were fully ingested. This helps with checking if a file needs to be ingested or not, as there is no 1 to 1 correspondense with embeddings to file (which is many to one).

Because JabRef is not build around one global database, but rather it is a `.bib` file editor, a problem of identifying a `BibEntry` arose and it was solved in a somewhat complicated way:

- In order to uniquely identify a library, an "AI library ID" was introduced (as a metadata field), which is just a UUID. An alternative would be to use the library path, but if the library moves, the path changes, but AI library ID is not.
- In order to uniquely identify an entry, the citation key is used, but only if it is non-empty and unique.
- In some cases (that arise potentially often), the conditions above are not met (for example, a library is not saved - it does not have a path, or an entry does not have a citation key), however user is actively working on an entry. In this case the AI features have an *in-memory cache layer*. So whenever a chat or a summary is created for an entry, it is firstly interacted with the in-memory storage layer. The cache is flushed to the on-disk storage at the close of the JabRef.
- In order to uniquely identify a file, we use the file hash. An alternative would be to use the file path, but the file could be moved, or defined by a relative path. This is also useful when several libraries cite the same paper, and instead of ingesting 

## [OLD] Internal model (v1)

The model v1 differs from v2 by:

1. Fields of the chat messages and summaries were differently organized in the `MVStore`.
2. A `LinkedFile#getLink()` was used to identify a file.

To migrate from v1 to v2, the classes `ChatHistoryMigrationV1` and `SummariesMigrationV2` were made.

---

## How to add a new AI feature

This section describes the standard pattern used for AI features. Summarization (`AiSummary`, `InMemorySummaryCache`, etc.) is used throughout as a reference implementation.

---

### Layer 1 — Domain model (`model/` package)

- Create a pure data record for the result, e.g. `AiFeatureResult` (e.g. `AiSummary`).
- Create a stable *identifier* record for persistent keying, e.g. `AiFeatureIdentifier(libraryId, citationKey)`.
  The identifier requires both an AI library ID (from `BibDatabaseContext` metadata) and a citation key (or group name, depending on which entity you're building the feature for).

---

### Layer 2 — Background task (`TrackedBackgroundTask<Result>`)

`TrackedBackgroundTask` exposes `statusProperty()`, `resultProperty()`, and `exceptionProperty()`
as JavaFX `ObjectProperty` fields — the result and exception are stored permanently, so they can be read even after the task has finished.

- Extend `TrackedBackgroundTask<Result>` and implement `perform()` for the long-running work.
- **Critical ordering rule:** attach all status listeners to the task object *before* submitting it to `TaskExecutor`. If you attach listeners after `taskExecutor.execute(task)`, the task may already be done and no listener notification will ever fire.
- Use `task.onFinished(runnable)` and `task.onSuccess(consumer)` callbacks (both run on the JavaFX thread) for post-task housekeeping.
- **Do NOT write to the persistent repository directly** — the task should only generate the result. Persistence is handled by the RAM cache on application close.

---

### Layer 3 — RAM cache (e.g. `InMemoryFeatureCache`)

Some entries may lack a citation key, making persistent storage impossible. The RAM cache fills this gap.

```java
// IdentityHashMap: keys compared by reference (==), not equals().
// Works for entries with or without a citation key.
private final Map<BibEntry, CachedEntry> cache =
        Collections.synchronizedMap(new IdentityHashMap<>());
```

- The cache stores a `CachedEntry` internally, which includes all necessary information to work with the result and to flush it to on-disk storage at the end. Use `FullBibEntry` (which pairs a `BibEntry` with a `BibDatabaseContext`) as the key carrier.
- Put the result in the cache inside `task.onSuccess(result -> cache.put(fullEntry, result))`, which runs on the JavaFX thread after `onFinished` has already removed the task from the aggregator map.
- On library or application close, call `cache.close()` to persist valid entries.
- **All persistence happens here** — background tasks do NOT write directly to the repository. This ensures a single source of truth for when and how data is persisted.

---

### Layer 4 — Persistent repository (e.g. `MVStoreFeatureRepository`)

- Implement a `FeatureRepository` interface backed by `MVStoreBase`.
- Keyed by the feature's identifier (library ID + citation key).

---

### Layer 5 — Task aggregator (`*TaskAggregator`)

The aggregator ensures exactly one task runs per domain object at a time.

```java
public synchronized Task start(Request request) {
    return tasks.computeIfAbsent(request.fullEntry().entry(), _ -> {
        Task task = new Task(request);
        task.onFinished(() -> tasks.remove(request.fullEntry().entry())); // remove on finish
        task.onSuccess(result -> cache.put(request.fullEntry(), result)); // write to RAM
        taskExecutor.execute(task);
        return task;
    });
}

public synchronized Optional<Task> getTask(BibEntry entry) {
    return Optional.ofNullable(tasks.get(entry));
}
```

- Use `FullBibEntry` in the request to keep `BibEntry` and `BibDatabaseContext` together.
- `computeIfAbsent` is the deduplication mechanism — a second call for the same key while a task is running returns the existing task, not a new one.
- Mark all methods `synchronized`; `start` may be called from background threads (e.g. from a batch task or database listener).
- The callbacks `onFinished` and `onSuccess` are chained by `BackgroundTask.getOnSuccess()`: `onFinished` runs first, then `onSuccess`. Both run on the JavaFX thread.

---

### Layer 6 — Switchable state-machine ViewModel

`AiSummaryViewModel` is the reference implementation. Key patterns:

#### 6a. Priority-ordered state binding

```java
BindingsHelper.bindEnum(
    state,
    State.PRECONDITION_NOT_MET, someCondition,
    State.DONE,       result.isNotNull(),
    State.PROCESSING, currentTask.isNotNull(),
    State.ERROR,      error.isNotNull(),
    State.READY       // fallback — triggers processEntry()
);
```

`bindEnum` evaluates conditions top-to-bottom; the first truthy one wins. `READY` is the "otherwise" fallback that triggers actual processing.

#### 6b. Entry switching

```java
// Fires every time the entry property changes to a non-null value.
ListenersHelper.onChangeNonNull(entry, this::prepareForEntry);

// Fires only when the entry changes AND state is READY.
ListenersHelper.onChangeNonNullWhen(entry, state.isEqualTo(State.READY), this::processEntry);
```

`prepareForEntry()` always clears `currentTask`, the result, and `error` so the state resets cleanly. `processEntry()` runs only when all preconditions are satisfied.

#### 6c. Three-layer lookup in `processEntry()`

```java
private void processEntry(FullBibEntry fullEntry) {
    // 1. Persistent storage — fast path for entries with a valid citation key
    Optional<AiFeatureResult> persisted = fullEntry.toAiFeatureIdentifier()
                                                   .flatMap(repository::get);
    if (persisted.isPresent()) { result.set(persisted.get()); return; }

    // 2. RAM cache — handles entries without a citation key and the "task finished
    //    while this view was switched away" race condition
    Optional<AiFeatureResult> cached = inMemoryCache.get(fullEntry.entry());
    if (cached.isPresent()) { result.set(cached.get()); return; }

    // 3. Reconnect to a running task (no duplicate starts)
    Optional<GenerateFeatureTask> running = aggregator.getTask(fullEntry.entry());
    if (running.isPresent()) {
        GenerateFeatureTask task = running.get();
        currentTask.set(task);
        // Immediate check: task may have finished in the window before the listener attached
        switch (task.getStatus()) {
            case SUCCESS -> { result.set(task.getResult()); clearTask(); }
            case ERROR   -> { error.set(task.getException()); clearTask(); }
            default      -> {} // PENDING/PROCESSING: taskStateListener handles it
        }
        return;
    }

    // 4. Nothing found — start a new task
    generate();
}
```

#### 6d. Detaching from a finished task

In `updateByTaskState`, capture the task reference *before* `runInJavaFXThread` to avoid a null-pointer race (another entry switch may have called `clearTask()` between the background thread notification and the FX thread runnable executing):

```java
private void updateByTaskState(TrackedBackgroundTask.Status value) {
    GenerateFeatureTask task = currentTask.get(); // capture before FX handoff
    if (task == null) { return; }
    UiTaskExecutor.runInJavaFXThread(() -> {
        switch (value) {
            case SUCCESS -> { result.set(task.getResult()); clearTask(); }
            case ERROR   -> { error.set(task.getException()); clearTask(); }
        }
    });
}
```

`clearTask()` sets `currentTask = null`, which causes `BindingsHelper.bindInternalListener` to automatically remove `taskStateListener` from the old task's `statusProperty`. The task is then free for GC.