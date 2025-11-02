# Data Cleanup Operations

This guide outlines the process for implementing data cleanup operations in JabRef, including field transformations, validation, and batch processing.

## Cleanup Operation Types

JabRef supports various types of cleanup operations that transform and normalize bibliography data:

### Field Transformations
- **DOI Cleanup**: Normalize DOI formats and resolve URLs
- **URL Cleanup**: Fix malformed URLs and standardize formats
- **Author Name Cleanup**: Normalize author name formats
- **Date Cleanup**: Standardize date formats and fix inconsistencies

### File and Link Management
- **File Links Cleanup**: Update file paths and remove broken links
- **PDF Cleanup**: Rename PDF files according to citation keys
- **Relative Paths Cleanup**: Convert absolute paths to relative paths

### Format Conversions
- **BibTeX/BibLaTeX Conversion**: Convert between bibliography formats
- **Field Format Cleanup**: Standardize field formatting and encoding
- **Whitespace Cleanup**: Remove unnecessary whitespace and normalize spacing

## Implementation Process

### 1. Cleanup Operation Planning
- Identify the data quality issue to address
- Determine the scope (single entries vs. entire database)
- Define transformation rules and edge cases
- Plan user confirmation requirements

### 2. Base Class Selection
Choose appropriate base class based on cleanup scope:

```java
// For field-level cleanup
public class DoiCleanup extends FieldFormatterCleanup {
    // Implementation
}

// For entry-level cleanup
public class ConvertToBiblatexCleanup implements CleanupJob {
    // Implementation
}

// For file-based cleanup
public class RenamePdfCleanup implements CleanupJob {
    // Implementation
}
```

### 3. Core Implementation
Implement the cleanup logic:

```java
@Override
public List<FieldChange> cleanup(BibEntry entry) {
    List<FieldChange> changes = new ArrayList<>();

    // Check if cleanup is applicable
    if (shouldCleanup(entry)) {
        // Perform cleanup
        FieldChange change = cleanupField(entry);
        if (change != null) {
            changes.add(change);
        }
    }

    return changes;
}

private boolean shouldCleanup(BibEntry entry) {
    // Check entry type, field presence, etc.
    return entry.hasField(StandardField.DOI) &&
           !isValidDoi(entry.getField(StandardField.DOI).orElse(""));
}

private FieldChange cleanupField(BibEntry entry) {
    String oldValue = entry.getField(StandardField.DOI).orElse("");
    String newValue = normalizeDoi(oldValue);

    if (!oldValue.equals(newValue)) {
        entry.setField(StandardField.DOI, newValue);
        return new FieldChange(entry, StandardField.DOI, oldValue, newValue);
    }

    return null;
}
```

## Field Formatter Cleanup Pattern

For field-level transformations, extend `FieldFormatterCleanup`:

```java
public class DoiCleanup extends FieldFormatterCleanup {

    public DoiCleanup() {
        super(StandardField.DOI, new DoiFormatter());
    }

    private static class DoiFormatter extends Formatter {
        @Override
        public String format(String value) {
            // DOI normalization logic
            return normalizeDoi(value);
        }

        @Override
        public String getName() {
            return "DOI Normalizer";
        }

        @Override
        public String getKey() {
            return "doi_normalizer";
        }

        @Override
        public String getDescription() {
            return "Normalizes DOI formats to standard representation";
        }
    }
}
```

## Cleanup Job Interface

For complex cleanup operations, implement `CleanupJob`:

```java
public class ComplexCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        // Multiple field transformations
        changes.addAll(cleanupAuthors(entry));
        changes.addAll(cleanupUrls(entry));
        changes.addAll(cleanupDates(entry));

        return changes;
    }

    @Override
    public String getDescription() {
        return "Comprehensive entry cleanup";
    }
}
```

## Error Handling and Validation

### Safe Transformations
```java
private String safeTransform(String value, Function<String, String> transformer) {
    try {
        return transformer.apply(value);
    } catch (Exception e) {
        // Log error but keep original value
        logger.warn("Failed to transform value: " + value, e);
        return value;
    }
}
```

### Validation Checks
```java
private boolean isValidTransformation(String oldValue, String newValue) {
    // Check for data loss
    if (newValue.length() < oldValue.length() * 0.8) {
        return false; // Potential data loss
    }

    // Check for suspicious changes
    if (containsSuspiciousPatterns(newValue)) {
        return false;
    }

    return true;
}
```

## Preferences Integration

### Cleanup Preferences
```java
public class CleanupPreferences {
    private final BooleanProperty cleanupDoi = new SimpleBooleanProperty(true);
    private final BooleanProperty cleanupUrls = new SimpleBooleanProperty(true);
    private final BooleanProperty cleanupAuthors = new SimpleBooleanProperty(false);

    // Property accessors
}
```

### User Confirmation
```java
public enum CleanupAction {
    CLEANUP_DOI("Normalize DOI formats"),
    CLEANUP_URL("Fix URL formats"),
    CLEANUP_AUTHORS("Normalize author names");

    private final String description;

    CleanupAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

## Batch Processing

### Cleanup Worker
```java
public class CleanupWorker {
    private final List<BibEntry> entries;
    private final List<CleanupJob> cleanupJobs;

    public CleanupResult cleanup() {
        CleanupResult result = new CleanupResult();

        for (BibEntry entry : entries) {
            for (CleanupJob job : cleanupJobs) {
                try {
                    List<FieldChange> changes = job.cleanup(entry);
                    result.addChanges(changes);
                } catch (Exception e) {
                    result.addError(entry, e);
                }
            }
        }

        return result;
    }
}
```

### Progress Tracking
```java
public class CleanupProgress {
    private final int totalEntries;
    private int processedEntries = 0;

    public void updateProgress(BibEntry entry, List<FieldChange> changes) {
        processedEntries++;
        double progress = (double) processedEntries / totalEntries;

        // Update UI progress
        updateProgressBar(progress);

        // Log changes
        if (!changes.isEmpty()) {
            logChanges(entry, changes);
        }
    }
}
```

## Testing Cleanup Operations

### Unit Tests
```java
@Test
void cleanupNormalizesDoi() {
    BibEntry entry = new BibEntry(StandardEntryType.Article);
    entry.setField(StandardField.DOI, "10.1000/example.doi");

    DoiCleanup cleanup = new DoiCleanup();
    List<FieldChange> changes = cleanup.cleanup(entry);

    assertEquals(1, changes.size());
    assertEquals("https://doi.org/10.1000/example.doi",
                 entry.getField(StandardField.DOI).orElse(""));
}
```

### Integration Tests
```java
@Test
void cleanupPreservesDataIntegrity() {
    // Load test database
    BibDatabase database = loadTestDatabase();

    // Apply cleanup
    CleanupWorker worker = new CleanupWorker(database.getEntries(), getCleanupJobs());
    CleanupResult result = worker.cleanup();

    // Verify no data loss
    assertTrue(result.getErrors().isEmpty());

    // Verify transformations are correct
    for (FieldChange change : result.getChanges()) {
        assertTrue(isValidChange(change));
    }
}
```

## Common Cleanup Patterns

### URL Normalization
```java
private String normalizeUrl(String url) {
    if (url == null || url.trim().isEmpty()) {
        return url;
    }

    String normalized = url.trim();

    // Add protocol if missing
    if (!normalized.matches("(?i)^https?://.*")) {
        normalized = "https://" + normalized;
    }

    // Fix common typos
    normalized = normalized.replace("http//", "http://");
    normalized = normalized.replace("https//", "https://");

    return normalized;
}
```

### Author Name Normalization
```java
private String normalizeAuthor(String author) {
    try {
        AuthorList authorList = AuthorList.parse(author);
        return authorList.getAsFirstLastNamesWithAnd();
    } catch (Exception e) {
        // Keep original if parsing fails
        return author;
    }
}
```

### Date Standardization
```java
private String standardizeDate(String date) {
    // Parse various date formats
    for (DateTimeFormatter formatter : DATE_FORMATTERS) {
        try {
            LocalDate parsed = LocalDate.parse(date, formatter);
            return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            // Try next formatter
        }
    }

    // Return original if no formatter matches
    return date;
}
```

## Performance Considerations

### Efficient Processing
```java
public class BatchedCleanup implements CleanupJob {
    private static final int BATCH_SIZE = 100;

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        // Process in batches to avoid memory issues
        List<FieldChange> allChanges = new ArrayList<>();

        for (List<BibEntry> batch : partitionEntries(entries, BATCH_SIZE)) {
            List<FieldChange> batchChanges = processBatch(batch);
            allChanges.addAll(batchChanges);
        }

        return allChanges;
    }
}
```

### Caching Results
```java
public class CachedCleanup extends FieldFormatterCleanup {
    private final Cache<String, String> transformationCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Override
    protected String transformField(String value) {
        return transformationCache.get(value, () -> super.transformField(value));
    }
}
```

## Undo/Redo Support

### Change Tracking
```java
public class UndoableCleanup implements CleanupJob {
    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        // Track original values for undo
        Map<Field, String> originalValues = new HashMap<>();
        for (Field field : entry.getFields()) {
            originalValues.put(field, entry.getField(field).orElse(""));
        }

        // Perform cleanup
        performCleanup(entry);

        // Create undo information
        for (Field field : entry.getFields()) {
            String newValue = entry.getField(field).orElse("");
            String oldValue = originalValues.get(field);

            if (!Objects.equals(oldValue, newValue)) {
                changes.add(new FieldChange(entry, field, oldValue, newValue));
            }
        }

        return changes;
    }
}
```

## Integration with UI

### Progress Dialog
```java
public class CleanupDialog extends FXDialog {
    private final ProgressBar progressBar;
    private final Label statusLabel;

    public CleanupDialog(List<BibEntry> entries, List<CleanupJob> jobs) {
        // Setup UI
        progressBar = new ProgressBar();
        statusLabel = new Label("Preparing cleanup...");

        // Setup cleanup task
        CleanupTask task = new CleanupTask(entries, jobs);
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        // Start cleanup
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
```

### Results Display
```java
public class CleanupResultsDialog extends FXDialog {
    public CleanupResultsDialog(CleanupResult result) {
        TableView<FieldChange> changesTable = createChangesTable(result.getChanges());
        Label summaryLabel = new Label(createSummary(result));

        VBox content = new VBox(10, summaryLabel, changesTable);
        setContent(content);
    }

    private String createSummary(CleanupResult result) {
        return String.format("Cleanup completed: %d changes made, %d errors",
                           result.getChanges().size(), result.getErrors().size());
    }
}
```

## Maintenance Guidelines

### Adding New Cleanup Operations
1. Identify the data quality issue
2. Implement the cleanup logic
3. Add comprehensive tests
4. Update cleanup preferences
5. Document the new operation

### Testing Cleanup Operations
- Test with various data formats
- Verify no data loss occurs
- Check edge cases and error conditions
- Validate undo/redo functionality

### Performance Monitoring
- Monitor cleanup execution time
- Track memory usage for large databases
- Optimize batch processing parameters
- Cache expensive transformations
