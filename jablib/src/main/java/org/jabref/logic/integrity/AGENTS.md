# Data Integrity Checkers

This guide outlines the process for implementing data integrity checkers in JabRef, including field validation, error reporting, and checker registration.

## Integrity Checker Types

JabRef uses integrity checkers to validate bibliography data and identify potential issues:

### Field Format Checkers
- **DOI Checkers**: Validate DOI format and accessibility
- **URL Checkers**: Verify URL format and reachability
- **ISBN Checkers**: Validate ISBN format and checksums
- **Date Checkers**: Verify date format consistency

### Content Validation Checkers
- **Author Name Checkers**: Validate author name formats
- **Citation Key Checkers**: Ensure unique and valid citation keys
- **Journal Name Checkers**: Verify journal abbreviations
- **Page Number Checkers**: Validate page range formats

### Cross-Reference Checkers
- **File Link Checkers**: Verify linked files exist
- **Entry Link Checkers**: Validate cross-references between entries
- **Duplication Checkers**: Identify duplicate entries or fields

## Implementation Process

### 1. Checker Planning
- Identify the validation requirement and scope
- Determine the field(s) to validate
- Define validation rules and error conditions
- Plan error message formatting

### 2. Base Class Selection
Choose appropriate base class based on validation scope:

```java
// For single field validation
public class DoiValidityChecker implements FieldChecker {
    // Implementation
}

// For entry-level validation
public class CitationKeyDuplicationChecker implements EntryChecker {
    // Implementation
}

// For database-level validation
public class DatabaseChecker implements Checker {
    // Implementation
}
```

### 3. Core Implementation
Implement the validation logic:

```java
public class DoiValidityChecker implements FieldChecker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> messages = new ArrayList<>();

        Optional<String> doi = entry.getField(StandardField.DOI);
        if (doi.isPresent()) {
            String doiValue = doi.get();
            if (!isValidDoiFormat(doiValue)) {
                messages.add(new IntegrityMessage(
                    "Invalid DOI format",
                    entry,
                    StandardField.DOI
                ));
            }
        }

        return messages;
    }

    @Override
    public String getDescription() {
        return "Validates DOI format and accessibility";
    }
}
```

## Field Checker Interface

For field-level validation, implement `FieldChecker`:

```java
public class UrlChecker implements FieldChecker {

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    );

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> messages = new ArrayList<>();

        for (Field field : getUrlFields()) {
            entry.getField(field).ifPresent(url -> {
                if (!isValidUrl(url)) {
                    messages.add(new IntegrityMessage(
                        "Invalid URL format",
                        entry,
                        field
                    ));
                }
            });
        }

        return messages;
    }

    private boolean isValidUrl(String url) {
        return URL_PATTERN.matcher(url).matches();
    }

    private List<Field> getUrlFields() {
        return List.of(StandardField.URL, StandardField.FILE, StandardField.PDF);
    }
}
```

## Entry Checker Interface

For entry-level validation, implement `EntryChecker`:

```java
public class CitationKeyDuplicationChecker implements EntryChecker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        // This checker needs database context
        // Implementation would check against all entries
        return List.of();
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry, BibDatabase database) {
        List<IntegrityMessage> messages = new ArrayList<>();

        String citationKey = entry.getCitationKey().orElse("");
        if (!citationKey.isEmpty()) {
            long duplicateCount = database.getEntries().stream()
                    .filter(other -> !other.equals(entry))
                    .mapToLong(other -> other.getCitationKey()
                            .filter(key -> key.equals(citationKey))
                            .isPresent() ? 1L : 0L)
                    .sum();

            if (duplicateCount > 0) {
                messages.add(new IntegrityMessage(
                    "Duplicate citation key found",
                    entry,
                    citationKey
                ));
            }
        }

        return messages;
    }
}
```

## Integrity Message System

### Message Creation
```java
public class IntegrityMessage {
    private final String message;
    private final BibEntry entry;
    private final Field field;
    private final IntegrityMessageType type;

    public IntegrityMessage(String message, BibEntry entry, Field field) {
        this(message, entry, field, IntegrityMessageType.ERROR);
    }

    public IntegrityMessage(String message, BibEntry entry, Field field, IntegrityMessageType type) {
        this.message = message;
        this.entry = entry;
        this.field = field;
        this.type = type;
    }

    // Getters and utility methods
}
```

### Message Types
```java
public enum IntegrityMessageType {
    ERROR("Error"),      // Critical issues that should be fixed
    WARNING("Warning"),  // Potential issues that may be acceptable
    INFO("Info");        // Informational messages

    private final String displayName;

    IntegrityMessageType(String displayName) {
        this.displayName = displayName;
    }
}
```

## Checker Registration

### Field Checkers Registration
```java
public class FieldCheckers {
    private static final List<FieldChecker> CHECKERS = List.of(
        new DoiValidityChecker(),
        new UrlChecker(),
        new ISBNChecker(),
        new PersonNamesChecker(),
        new BracesCorrector(),
        new HTMLCharacterChecker(),
        new JournalInAbbreviationListChecker()
    );

    public static List<FieldChecker> getAll() {
        return CHECKERS;
    }

    public static List<FieldChecker> getForField(Field field) {
        return CHECKERS.stream()
                .filter(checker -> appliesToField(checker, field))
                .collect(Collectors.toList());
    }
}
```

### Entry Checkers Registration
```java
public class EntryCheckers {
    private static final List<EntryChecker> CHECKERS = List.of(
        new CitationKeyDuplicationChecker(),
        new EntryLinkChecker(),
        new DatabaseChecker()
    );

    public static List<EntryChecker> getAll() {
        return CHECKERS;
    }
}
```

## Asynchronous Validation

### Background Checking
```java
public class AsyncIntegrityChecker {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public CompletableFuture<List<IntegrityMessage>> checkAsync(BibEntry entry, BibDatabase database) {
        return CompletableFuture.supplyAsync(() -> {
            List<IntegrityMessage> messages = new ArrayList<>();

            // Run field checkers
            for (FieldChecker checker : FieldCheckers.getAll()) {
                messages.addAll(checker.check(entry));
            }

            // Run entry checkers
            for (EntryChecker checker : EntryCheckers.getAll()) {
                messages.addAll(checker.check(entry, database));
            }

            return messages;
        }, executor);
    }
}
```

### Progress Tracking
```java
public class IntegrityCheckProgress {
    private final int totalEntries;
    private int checkedEntries = 0;

    public void updateProgress(int batchSize) {
        checkedEntries += batchSize;
        double progress = (double) checkedEntries / totalEntries;

        // Update UI
        Platform.runLater(() -> updateProgressBar(progress));
    }
}
```

## Error Handling and Recovery

### Safe Validation
```java
private List<IntegrityMessage> safeCheck(BibEntry entry, Checker checker) {
    try {
        return checker.check(entry);
    } catch (Exception e) {
        // Log error and return empty list
        logger.warn("Checker failed: " + checker.getClass().getSimpleName(), e);
        return List.of(new IntegrityMessage(
            "Checker failed: " + e.getMessage(),
            entry,
            IntegrityMessageType.WARNING
        ));
    }
}
```

### Timeout Handling
```java
public class TimeoutChecker implements FieldChecker {
    private static final int TIMEOUT_MS = 5000; // 5 seconds

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        CompletableFuture<List<IntegrityMessage>> future = checkAsync(entry);

        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return List.of(new IntegrityMessage(
                "Validation timeout",
                entry,
                IntegrityMessageType.WARNING
            ));
        } catch (Exception e) {
            return List.of(new IntegrityMessage(
                "Validation failed: " + e.getMessage(),
                entry,
                IntegrityMessageType.ERROR
            ));
        }
    }
}
```

## Testing Integrity Checkers

### Unit Tests
```java
@Test
void doiCheckerDetectsInvalidFormat() {
    BibEntry entry = new BibEntry(StandardEntryType.Article);
    entry.setField(StandardField.DOI, "invalid-doi-format");

    DoiValidityChecker checker = new DoiValidityChecker();
    List<IntegrityMessage> messages = checker.check(entry);

    assertEquals(1, messages.size());
    assertEquals("Invalid DOI format", messages.get(0).getMessage());
    assertEquals(StandardField.DOI, messages.get(0).getField());
}
```

### Integration Tests
```java
@Test
void integrityCheckFindsMultipleIssues() {
    BibDatabase database = createTestDatabaseWithIssues();

    IntegrityCheck check = new IntegrityCheck(database);
    List<IntegrityMessage> messages = check.checkAll();

    // Verify expected issues are found
    assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("DOI")));
    assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("URL")));
    assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("citation key")));
}
```

## Common Validation Patterns

### Pattern-Based Validation
```java
public class PatternChecker implements FieldChecker {
    private final Pattern pattern;
    private final String errorMessage;

    public PatternChecker(String regex, String errorMessage) {
        this.pattern = Pattern.compile(regex);
        this.errorMessage = errorMessage;
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        // Implementation for pattern-based checking
        return entry.getFields().stream()
                .filter(field -> shouldCheckField(field))
                .flatMap(field -> checkField(entry, field).stream())
                .collect(Collectors.toList());
    }

    private List<IntegrityMessage> checkField(BibEntry entry, Field field) {
        return entry.getField(field)
                .filter(value -> !pattern.matcher(value).matches())
                .map(value -> new IntegrityMessage(errorMessage, entry, field))
                .map(List::of)
                .orElse(List.of());
    }
}
```

### External Service Validation
```java
public class ExternalValidator implements FieldChecker {
    private final HttpClient httpClient;

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> messages = new ArrayList<>();

        entry.getField(StandardField.DOI).ifPresent(doi -> {
            if (!isDoiAccessible(doi)) {
                messages.add(new IntegrityMessage(
                    "DOI not accessible",
                    entry,
                    StandardField.DOI,
                    IntegrityMessageType.WARNING
                ));
            }
        });

        return messages;
    }

    private boolean isDoiAccessible(String doi) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://doi.org/" + doi))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.discarding());

            return response.statusCode() == 200;
        } catch (Exception e) {
            return false; // Assume not accessible if check fails
        }
    }
}
```

### Cross-Entry Validation
```java
public class CrossEntryChecker implements Checker {
    @Override
    public List<IntegrityMessage> check(BibDatabase database) {
        List<IntegrityMessage> messages = new ArrayList<>();
        Map<String, List<BibEntry>> entriesByKey = groupByCitationKey(database);

        // Check for duplicates
        for (Map.Entry<String, List<BibEntry>> entry : entriesByKey.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (BibEntry duplicate : entry.getValue()) {
                    messages.add(new IntegrityMessage(
                        "Duplicate citation key: " + entry.getKey(),
                        duplicate,
                        IntegrityMessageType.ERROR
                    ));
                }
            }
        }

        return messages;
    }

    private Map<String, List<BibEntry>> groupByCitationKey(BibDatabase database) {
        return database.getEntries().stream()
                .filter(entry -> entry.getCitationKey().isPresent())
                .collect(Collectors.groupingBy(
                    entry -> entry.getCitationKey().get()
                ));
    }
}
```

## Performance Optimization

### Caching Results
```java
public class CachedChecker implements FieldChecker {
    private final Cache<String, Boolean> validationCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return entry.getFields().stream()
                .flatMap(field -> checkFieldCached(entry, field).stream())
                .collect(Collectors.toList());
    }

    private List<IntegrityMessage> checkFieldCached(BibEntry entry, Field field) {
        return entry.getField(field)
                .map(value -> validationCache.get(value, () -> validateField(value)))
                .filter(isValid -> !isValid)
                .map(value -> List.of(new IntegrityMessage("Invalid field", entry, field)))
                .orElse(List.of());
    }
}
```

### Parallel Processing
```java
public class ParallelIntegrityChecker {
    private final ExecutorService executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    public List<IntegrityMessage> checkParallel(List<BibEntry> entries) {
        List<CompletableFuture<List<IntegrityMessage>>> futures = entries.stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> checkEntry(entry), executor))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
```

## UI Integration

### Results Display
```java
public class IntegrityResultsView {
    private final TableView<IntegrityMessage> resultsTable;

    public IntegrityResultsView(List<IntegrityMessage> messages) {
        resultsTable = createResultsTable();

        // Group by type
        Map<IntegrityMessageType, List<IntegrityMessage>> grouped = messages.stream()
                .collect(Collectors.groupingBy(IntegrityMessage::getType));

        // Display summary
        updateSummary(grouped);

        // Populate table
        resultsTable.getItems().setAll(messages);
    }

    private void updateSummary(Map<IntegrityMessageType, List<IntegrityMessage>> grouped) {
        int errors = grouped.getOrDefault(IntegrityMessageType.ERROR, List.of()).size();
        int warnings = grouped.getOrDefault(IntegrityMessageType.WARNING, List.of()).size();

        summaryLabel.setText(String.format("Found %d errors, %d warnings", errors, warnings));
    }
}
```

### Real-time Checking
```java
public class LiveIntegrityChecker {
    private final BibEntry entry;
    private final List<FieldChecker> checkers;

    public LiveIntegrityChecker(BibEntry entry) {
        this.entry = entry;
        this.checkers = FieldCheckers.getAll();
    }

    public void startLiveChecking() {
        entry.getFields().forEach(field -> {
            entry.getFieldBinding(field).addListener((obs, oldValue, newValue) -> {
                checkFieldLive(field, newValue);
            });
        });
    }

    private void checkFieldLive(Field field, String newValue) {
        List<FieldChecker> relevantCheckers = FieldCheckers.getForField(field);

        for (FieldChecker checker : relevantCheckers) {
            // Run quick validation and update UI
            boolean isValid = quickValidate(checker, newValue);
            updateFieldValidationUI(field, isValid);
        }
    }
}
```

## Maintenance Guidelines

### Adding New Checkers
1. Identify the validation requirement
2. Implement the checker logic
3. Add comprehensive tests
4. Register the checker appropriately
5. Update documentation

### Checker Performance
- Profile checker execution time
- Optimize expensive validations
- Consider caching for repeated checks
- Implement timeouts for external services

### Error Message Consistency
- Use clear, actionable language
- Include context about the issue
- Suggest fixes when possible
- Maintain consistent tone and format
