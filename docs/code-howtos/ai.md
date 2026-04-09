- ai package organization
- gui / logic / model
- how to make ui -> view view-model pattern
- currents
- task organization
- generic classess
- long task and shutdown signal
- ensure to use ChatModel from jabref


how to use MVStores:
general dissection:
1. MVStore - for different repositories. 
2. MVMap - (for entry chats) - one for "databasePath + entryID".
3. Entry - specific chunks and chathistory records with their ID (UUID).
Messages/Chunks should be distinct/separate/atomic. E.g. we don't store a List of messages in an entry.

I would like a forth layer: to distrinct database and entry, but it is what it is.

try to group repositories for one type of data. No: entry and group chat history repostiory, yes: single chat history repository.


explain how messages are stored (v1 and v2).



logical flaw in task aggregation: yoou need to add status listeners BEFORE executing.

---

## How to add a new AI feature

This section describes the standard pattern used for AI features like summarization.
Follow these layers, in order from bottom to top.

---

### Layer 1 — Domain model (`model/` package)

- Create a pure data record for the result, e.g. `AiSummary`.
- Create a stable *identifier* record for persistent keying, e.g. `AiSummaryIdentifier(libraryId, citationKey)`.
  The identifier requires both an AI library ID (from `BibDatabaseContext` metadata) and a citation key.
  Provide a safe factory: `Optional<Identifier> from(ctx, entry)` that returns `Optional.empty()` when
  either part is absent.
- Use `FullBibEntry(databaseContext, entry)` to group a `BibEntry` with its `BibDatabaseContext` — this
  eliminates error-prone parameter pairs and provides utility methods like `toAiSummaryIdentifier()`.

---

### Layer 2 — Background task (`TrackedBackgroundTask<Result>`)

- Extend `TrackedBackgroundTask<Result>` and implement `perform()` for the long-running work.
- `TrackedBackgroundTask` exposes `statusProperty()`, `resultProperty()`, and `exceptionProperty()`
  as JavaFX `ObjectProperty` fields — the result and exception are stored permanently, so they can be
  read even after the task has finished.
- Call `showToUser(true)` in the constructor to show the task in the progress UI.
- **Critical ordering rule:** attach all status listeners to the task object *before* submitting it
  to `TaskExecutor`. If you attach listeners after `taskExecutor.execute(task)`, the task may already
  be done and no listener notification will ever fire.
- Use `task.onFinished(runnable)` and `task.onSuccess(consumer)` callbacks (both run on the JavaFX
  thread) for post-task housekeeping.
- **Do NOT write to the persistent repository directly** — the task should only generate the result.
  Persistence is handled by the RAM cache on application close.

---

### Layer 3 — RAM cache (`InMemorySummaryCache` pattern)

Some entries may lack a citation key, making persistent storage impossible. The RAM cache fills this gap.

```java
// IdentityHashMap: keys compared by reference (==), not equals().
// Works for entries with or without a citation key.
private final Map<BibEntry, CachedEntry> cache =
        Collections.synchronizedMap(new IdentityHashMap<>());
```

- The cache stores a `FullBibEntry` internally, not just the entry, so it can flush correctly.
- Put the result in the cache inside `task.onSuccess(result -> cache.put(fullEntry, result))`.
  This runs on the JavaFX thread, after `onFinished` has already removed the task from the aggregator map.
- On library or application close, call `cache.close()` to persist valid entries.
  The flush uses `fullEntry.toAiSummaryIdentifier()` — entries without a valid identifier are
  silently skipped.
- **All persistence happens here** — background tasks do NOT write directly to the repository.
  This ensures a single source of truth for when and how data is persisted.

---

### Layer 4 — Persistent repository (`MVStoreSummariesRepository`)

- Implement `SummariesRepository` backed by `MVStoreBase`.
- Keyed by `AiSummaryIdentifier` (library ID + citation key).
- Use `ObjectMapper` with `JavaTimeModule` to serialize the result record to JSON.
- Survives restarts; **only written to by the RAM cache's `close()` method** — background tasks
  do NOT write directly to the repository.

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
- `computeIfAbsent` is the deduplication mechanism. A second call for the same key while a task is
  running returns the existing task, not a new one.
- Mark all methods `synchronized` — `start` may be called from background threads
  (e.g. from a batch task or database listener).
- The callbacks `onFinished` and `onSuccess` are chained by `BackgroundTask.getOnSuccess()`:
  `onFinished` runs first, then `onSuccess`. Both run on the JavaFX thread.

---

### Layer 6 — Switchable state-machine ViewModel

`AiSummaryViewModel` is the reference implementation. Key patterns:

#### 6a. Priority-ordered state binding

```java
BindingsHelper.bindEnum(
    state,
    State.PRECONDITION_NOT_MET, someCondition,
    State.DONE,       summary.isNotNull(),
    State.PROCESSING, currentTask.isNotNull(),
    State.ERROR,      error.isNotNull(),
    State.READY       // fallback — triggers processEntry()
);
```

`bindEnum` evaluates conditions top-to-bottom; the first truthy one wins.
`READY` is the "otherwise" fallback that triggers actual processing.

#### 6b. Entry switching

```java
// Fires every time the entry property changes to a non-null value.
ListenersHelper.onChangeNonNull(entry, this::prepareForEntry);

// Fires only when the entry changes AND state is READY.
ListenersHelper.onChangeNonNullWhen(entry, state.isEqualTo(State.READY), this::processEntry);
```

`prepareForEntry()` always clears `currentTask`, `summary`, and `error` so the state resets cleanly.
`processEntry()` runs only when all preconditions are satisfied.

#### 6c. Three-layer lookup in `processEntry()`

```java
private void processEntry(FullBibEntry fullEntry) {
    // 1. Persistent storage — fast path for entries with a valid citation key
    Optional<AiSummary> persisted = fullEntry.toAiSummaryIdentifier()
                                             .flatMap(repository::get);
    if (persisted.isPresent()) { summary.set(persisted.get()); return; }

    // 2. RAM cache — handles entries without a citation key and the "task finished
    //    while this view was switched away" race condition
    Optional<AiSummary> cached = inMemoryCache.get(fullEntry.entry());
    if (cached.isPresent()) { summary.set(cached.get()); return; }

    // 3. Reconnect to a running task (no duplicate starts)
    Optional<GenerateSummaryTask> running = aggregator.getTask(fullEntry.entry());
    if (running.isPresent()) {
        GenerateSummaryTask task = running.get();
        currentTask.set(task);
        // Immediate check: task may have finished in the window before the listener attached
        switch (task.getStatus()) {
            case SUCCESS -> { summary.set(task.getResult()); clearTask(); }
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

In `updateByTaskState`, capture the task reference *before* `runInJavaFXThread` to avoid a null-pointer
race (another entry switch may have called `clearTask()` between the background thread notification and
the FX thread runnable executing):

```java
private void updateByTaskState(TrackedBackgroundTask.Status value) {
    GenerateSummaryTask task = currentTask.get(); // capture before FX handoff
    if (task == null) { return; }
    UiTaskExecutor.runInJavaFXThread(() -> {
        switch (value) {
            case SUCCESS -> { summary.set(task.getResult()); clearTask(); }
            case ERROR   -> { error.set(task.getException()); clearTask(); }
        }
    });
}
```

`clearTask()` sets `currentTask = null`, which causes `BindingsHelper.bindInternalListener` to
automatically remove `taskStateListener` from the old task's `statusProperty`. The task is then free
for GC.

---

### Ownership summary

| Class | Lives in | Owns |
|---|---|---|
| `AiSummary` | `model/` | pure data |
| `AiSummaryIdentifier` | `model/` | persistent key |
| `GenerateSummaryTask` | `logic/.../tasks/` | background work |
| `InMemorySummaryCache` | `logic/.../summarization/` | session RAM storage |
| `MVStoreSummariesRepository` | `logic/.../repositories/` | persistent storage |
| `SummarizationTaskAggregator` | `logic/.../summarization/` | deduplication + RAM write-back |
| `SummarizationAiFeature` | `logic/ai/` | wires layers together, owns lifecycle |
| `AiSummaryViewModel` | `gui/.../summary/` | state machine, lookup chain |
| `AiSummaryView` | `gui/.../summary/` | JavaFX bindings to ViewModel |
