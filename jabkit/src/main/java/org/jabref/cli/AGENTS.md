# CLI Commands

This guide outlines the process for implementing CLI commands in JabRef, including command parsing, execution, and user interaction.

## CLI Architecture

JabRef's command-line interface provides various operations for bibliography management:

### Command Categories
- **Import/Export**: Convert between formats and sources
- **Database Operations**: Search, modify, and analyze databases
- **Quality Assurance**: Check and fix bibliography data
- **Utility Operations**: Generate citations, validate files

### Command Components
- **Command Parser**: Parse command-line arguments and options
- **Command Executor**: Execute the requested operation
- **Output Formatter**: Format and display results
- **Error Handler**: Manage errors and provide user feedback

## Implementation Process

### 1. Command Planning
- Define the command purpose and target users
- Identify required and optional parameters
- Plan output format and error handling
- Consider performance implications for large datasets

### 2. Command Implementation
Create command classes following established patterns:

```java
@Command(name = "import", description = "Import bibliography from external source")
public class ImportCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Source file or URL to import from")
    private String source;

    @Option(names = {"-f", "--format"}, description = "Import format (bibtex, ris, endnote, etc.)")
    private String format;

    @Option(names = {"-o", "--output"}, description = "Output file (default: stdout)")
    private File outputFile;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        // Validate inputs
        if (source == null || source.trim().isEmpty()) {
            throw new ParameterException("Source is required");
        }

        // Determine format if not specified
        if (format == null) {
            format = detectFormat(source);
        }

        // Execute import
        try {
            List<BibEntry> entries = performImport(source, format);

            // Output results
            writeOutput(entries, outputFile);

            if (verbose) {
                System.err.println("Imported " + entries.size() + " entries");
            }

            return 0; // Success

        } catch (Exception e) {
            System.err.println("Import failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1; // Error
        }
    }

    private String detectFormat(String source) {
        // Auto-detect format based on file extension or content
        if (source.endsWith(".bib")) {
            return "bibtex";
        } else if (source.endsWith(".ris")) {
            return "ris";
        }
        // Default to bibtex
        return "bibtex";
    }

    private List<BibEntry> performImport(String source, String format) throws IOException {
        // Import logic here
        Importer importer = ImporterFactory.getImporter(format);
        try (InputStream input = openSource(source)) {
            ParserResult result = importer.importDatabase(input, StandardCharsets.UTF_8);
            return result.getDatabase().getEntries();
        }
    }

    private InputStream openSource(String source) throws IOException {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            // Handle URL sources
            return new URL(source).openStream();
        } else {
            // Handle file sources
            return new FileInputStream(source);
        }
    }

    private void writeOutput(List<BibEntry> entries, File outputFile) throws IOException {
        BibDatabase database = new BibDatabase(entries);
        BibtexExporter exporter = new BibtexExporter();

        if (outputFile != null) {
            try (FileWriter writer = new FileWriter(outputFile)) {
                exporter.export(database, outputFile.toPath(), StandardCharsets.UTF_8, entries);
            }
        } else {
            // Write to stdout
            exporter.export(database, System.out, StandardCharsets.UTF_8, entries);
        }
    }
}
```

### 3. Command Registration
Register commands with the CLI framework:

```java
public class JabRefCli {
    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new JabRefCommand())
                .addSubcommand("import", new ImportCommand())
                .addSubcommand("export", new ExportCommand())
                .addSubcommand("search", new SearchCommand())
                .addSubcommand("check", new CheckCommand());

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Command(name = "jabref", description = "JabRef command-line interface")
    static class JabRefCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("JabRef CLI - use --help for available commands");
        }
    }
}
```

## Command Patterns

### Data Processing Commands
```java
@Command(name = "normalize", description = "Normalize bibliography data")
public class NormalizeCommand implements Callable<Integer> {

    @Parameters(description = "Input bibliography files")
    private List<File> inputFiles;

    @Option(names = {"--doi"}, description = "Normalize DOI fields")
    private boolean normalizeDoi = true;

    @Option(names = {"--authors"}, description = "Normalize author names")
    private boolean normalizeAuthors = true;

    @Override
    public Integer call() throws Exception {
        CleanupWorker worker = new CleanupWorker();

        // Configure cleanup operations
        if (normalizeDoi) {
            worker.addCleanup(new DoiCleanup());
        }
        if (normalizeAuthors) {
            worker.addCleanup(new AuthorCleanup());
        }

        // Process each file
        for (File inputFile : inputFiles) {
            try {
                List<BibEntry> entries = loadEntries(inputFile);
                CleanupResult result = worker.cleanup(entries);
                saveEntries(entries, createOutputFile(inputFile));

                System.out.println("Normalized " + result.getChanges().size() +
                                 " fields in " + inputFile.getName());

            } catch (Exception e) {
                System.err.println("Failed to process " + inputFile + ": " + e.getMessage());
                return 1;
            }
        }

        return 0;
    }
}
```

### Analysis Commands
```java
@Command(name = "analyze", description = "Analyze bibliography database")
public class AnalyzeCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Bibliography file to analyze")
    private File bibFile;

    @Option(names = {"--stats"}, description = "Show statistics")
    private boolean showStats = true;

    @Option(names = {"--duplicates"}, description = "Find duplicate entries")
    private boolean findDuplicates = false;

    @Override
    public Integer call() throws Exception {
        List<BibEntry> entries = loadEntries(bibFile);

        if (showStats) {
            printStatistics(entries);
        }

        if (findDuplicates) {
            List<DuplicateGroup> duplicates = findDuplicates(entries);
            printDuplicates(duplicates);
        }

        return 0;
    }

    private void printStatistics(List<BibEntry> entries) {
        Map<String, Long> entryTypeStats = entries.stream()
                .collect(Collectors.groupingBy(
                    entry -> entry.getType().getName(),
                    Collectors.counting()
                ));

        System.out.println("Database Statistics:");
        System.out.println("Total entries: " + entries.size());
        entryTypeStats.forEach((type, count) ->
            System.out.println("  " + type + ": " + count));
    }

    private List<DuplicateGroup> findDuplicates(List<BibEntry> entries) {
        // Duplicate detection logic
        return DuplicateDetector.findDuplicates(entries);
    }
}
```

## Parameter Handling

### Complex Parameter Types
```java
@Command(name = "filter", description = "Filter bibliography entries")
public class FilterCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Input bibliography file")
    private File inputFile;

    @Option(names = {"-t", "--type"}, description = "Entry types to include")
    private List<String> entryTypes = new ArrayList<>();

    @Option(names = {"-y", "--year"}, description = "Year range (e.g., 2020-2023)")
    private String yearRange;

    @Option(names = {"-a", "--author"}, description = "Author name filter")
    private String authorFilter;

    @Override
    public Integer call() throws Exception {
        List<BibEntry> entries = loadEntries(inputFile);

        // Apply filters
        Stream<BibEntry> filteredStream = entries.stream();

        if (!entryTypes.isEmpty()) {
            filteredStream = filteredStream.filter(entry ->
                entryTypes.contains(entry.getType().getName()));
        }

        if (yearRange != null) {
            YearRange range = parseYearRange(yearRange);
            filteredStream = filteredStream.filter(entry ->
                isInYearRange(entry, range));
        }

        if (authorFilter != null) {
            filteredStream = filteredStream.filter(entry ->
                containsAuthor(entry, authorFilter));
        }

        List<BibEntry> filteredEntries = filteredStream.collect(Collectors.toList());

        // Output filtered results
        writeOutput(filteredEntries, createOutputFile(inputFile));

        System.out.println("Filtered " + entries.size() + " entries to " +
                         filteredEntries.size() + " entries");

        return 0;
    }

    private YearRange parseYearRange(String range) {
        String[] parts = range.split("-");
        return new YearRange(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private boolean isInYearRange(BibEntry entry, YearRange range) {
        return entry.getField(StandardField.YEAR)
                .map(year -> {
                    try {
                        int yearInt = Integer.parseInt(year);
                        return yearInt >= range.start() && yearInt <= range.end();
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .orElse(false);
    }

    private boolean containsAuthor(BibEntry entry, String authorFilter) {
        return entry.getField(StandardField.AUTHOR)
                .map(author -> author.toLowerCase().contains(authorFilter.toLowerCase()))
                .orElse(false);
    }
}
```

### File Input/Output Handling
```java
public class FileHandler {
    public static List<BibEntry> loadEntries(File file) throws IOException {
        BibtexImporter importer = new BibtexImporter();
        try (FileInputStream input = new FileInputStream(file)) {
            ParserResult result = importer.importDatabase(input, StandardCharsets.UTF_8);
            return result.getDatabase().getEntries();
        }
    }

    public static void saveEntries(List<BibEntry> entries, File file) throws IOException {
        BibDatabase database = new BibDatabase(entries);
        BibtexExporter exporter = new BibtexExporter();

        try (FileWriter writer = new FileWriter(file)) {
            exporter.export(database, file.toPath(), StandardCharsets.UTF_8, entries);
        }
    }

    public static File createOutputFile(File inputFile) {
        String baseName = inputFile.getName();
        if (baseName.endsWith(".bib")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        return new File(inputFile.getParent(), baseName + "_processed.bib");
    }
}
```

## Progress Reporting

### Progress Bars for Long Operations
```java
@Command(name = "process", description = "Process large bibliography files")
public class ProcessCommand implements Callable<Integer> {

    @Parameters(description = "Files to process")
    private List<File> files;

    @Override
    public Integer call() throws Exception {
        ConsoleProgressBar progressBar = new ConsoleProgressBar();

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            progressBar.updateProgress(i, files.size(), "Processing " + file.getName());

            try {
                processFile(file);
            } catch (Exception e) {
                progressBar.updateProgress(i + 1, files.size(),
                    "Failed: " + file.getName() + " - " + e.getMessage());
                return 1;
            }
        }

        progressBar.complete("Processing completed successfully");
        return 0;
    }
}
```

### Batch Processing
```java
public class BatchProcessor {
    private final int batchSize;

    public void processBatch(List<File> files, Consumer<File> processor) {
        for (int i = 0; i < files.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, files.size());
            List<File> batch = files.subList(i, endIndex);

            System.out.println("Processing batch " + (i / batchSize + 1) +
                             " (" + batch.size() + " files)");

            for (File file : batch) {
                try {
                    processor.accept(file);
                } catch (Exception e) {
                    System.err.println("Failed to process " + file + ": " + e.getMessage());
                }
            }
        }
    }
}
```

## Error Handling and Validation

### Input Validation
```java
public class InputValidator {
    public static void validateInputFile(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("Cannot read file: " + file);
        }
        if (!file.getName().endsWith(".bib")) {
            throw new IllegalArgumentException("File must have .bib extension: " + file);
        }
    }

    public static void validateYearRange(String yearRange) {
        if (!yearRange.matches("\\d{4}-\\d{4}")) {
            throw new IllegalArgumentException("Year range must be in format YYYY-YYYY");
        }

        String[] parts = yearRange.split("-");
        int startYear = Integer.parseInt(parts[0]);
        int endYear = Integer.parseInt(parts[1]);

        if (startYear > endYear) {
            throw new IllegalArgumentException("Start year must be before end year");
        }
    }
}
```

### Graceful Error Recovery
```java
public class ErrorHandler {
    public static int handleCommandError(Exception e, boolean verbose) {
        if (e instanceof ParameterException) {
            System.err.println("Invalid parameters: " + e.getMessage());
            System.err.println("Use --help for usage information");
            return 2;
        } else if (e instanceof IOException) {
            System.err.println("I/O error: " + e.getMessage());
            return 3;
        } else {
            System.err.println("Unexpected error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }
}
```

## Output Formatting

### Structured Output
```java
public class OutputFormatter {
    public static void printEntries(List<BibEntry> entries, boolean detailed) {
        if (detailed) {
            printDetailed(entries);
        } else {
            printSummary(entries);
        }
    }

    private static void printSummary(List<BibEntry> entries) {
        System.out.println("Found " + entries.size() + " entries:");
        for (BibEntry entry : entries) {
            String title = entry.getTitle().orElse("No title");
            String year = entry.getField(StandardField.YEAR).orElse("No year");
            System.out.println("  - " + title + " (" + year + ")");
        }
    }

    private static void printDetailed(List<BibEntry> entries) {
        BibtexExporter exporter = new BibtexExporter();
        for (BibEntry entry : entries) {
            try {
                StringWriter writer = new StringWriter();
                exporter.export(new BibDatabase(List.of(entry)), writer,
                              StandardCharsets.UTF_8, List.of(entry));
                System.out.println(writer.toString());
                System.out.println();
            } catch (Exception e) {
                System.err.println("Failed to format entry: " + e.getMessage());
            }
        }
    }
}
```

### JSON Output for Scripting
```java
@Option(names = {"--json"}, description = "Output results as JSON")
private boolean jsonOutput;

private void writeJsonOutput(List<BibEntry> entries) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    List<Map<String, Object>> jsonEntries = entries.stream()
            .map(this::entryToMap)
            .collect(Collectors.toList());

    Map<String, Object> output = Map.of(
        "count", entries.size(),
        "entries", jsonEntries
    );

    mapper.writeValue(System.out, output);
}

private Map<String, Object> entryToMap(BibEntry entry) {
    Map<String, Object> map = new HashMap<>();
    map.put("citationKey", entry.getCitationKey().orElse(""));
    map.put("entryType", entry.getType().getName());

    // Add all fields
    for (Field field : entry.getFields()) {
        entry.getField(field).ifPresent(value -> map.put(field.getName(), value));
    }

    return map;
}
```

## Testing CLI Commands

### Unit Tests
```java
@Test
void importCommandParsesArgumentsCorrectly() {
    // Test argument parsing
    ImportCommand command = new ImportCommand();
    CommandLine commandLine = new CommandLine(command);

    int exitCode = commandLine.execute("test.bib", "--format", "bibtex", "--verbose");

    assertEquals(0, exitCode);
    assertEquals("test.bib", command.source);
    assertEquals("bibtex", command.format);
    assertTrue(command.verbose);
}
```

### Integration Tests
```java
@Test
void importCommandProcessesFileCorrectly() throws IOException {
    // Create test file
    File testFile = createTestBibFile();

    // Run command
    ImportCommand command = new ImportCommand();
    command.source = testFile.getAbsolutePath();
    command.format = "bibtex";

    int exitCode = command.call();

    assertEquals(0, exitCode);
    // Verify output was generated correctly
}
```

### Error Handling Tests
```java
@Test
void importCommandHandlesMissingFile() {
    ImportCommand command = new ImportCommand();
    command.source = "nonexistent.bib";

    assertThrows(ParameterException.class, command::call);
}
```

## Performance Optimization

### Streaming Processing
```java
public class StreamingProcessor {
    public void processLargeFile(File inputFile, File outputFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String processedLine = processLine(line);
                writer.write(processedLine);
                writer.newLine();
            }
        }
    }
}
```

### Memory-Efficient Operations
```java
public class MemoryEfficientProcessor {
    private static final int CHUNK_SIZE = 1000;

    public void processInChunks(List<BibEntry> allEntries, Consumer<List<BibEntry>> processor) {
        for (int i = 0; i < allEntries.size(); i += CHUNK_SIZE) {
            int endIndex = Math.min(i + CHUNK_SIZE, allEntries.size());
            List<BibEntry> chunk = allEntries.subList(i, endIndex);

            processor.accept(chunk);

            // Allow GC to clean up
            System.gc();
        }
    }
}
```

## Maintenance Guidelines

### Adding New Commands
1. Identify the CLI need and target users
2. Design command interface and parameters
3. Implement command logic with proper error handling
4. Add comprehensive tests
5. Update help documentation
6. Register command in CLI framework

### Command Evolution
- Maintain backward compatibility for existing options
- Add new options with sensible defaults
- Deprecate old options with clear migration paths
- Update command help and documentation

### Performance Monitoring
- Profile command execution times
- Monitor memory usage for large datasets
- Optimize I/O operations
- Cache expensive computations when appropriate
