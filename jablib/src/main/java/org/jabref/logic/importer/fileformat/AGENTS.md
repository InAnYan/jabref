# Implementing Bibliography File Format Parsers

This guide outlines the process for implementing parsers for new bibliography file formats in JabRef, including format detection, parsing strategies, and validation.

## Parser Types and Architecture

JabRef supports multiple bibliography formats through a modular parser architecture:

### Format Categories

- **BibTeX/BibLaTeX**: Native JabRef formats
- **XML-based**: MODS, MARC, EndNote XML
- **Plain text**: RIS, ISI, Medline plain
- **Custom formats**: Publisher-specific formats

### Base Classes

Choose appropriate base class based on format characteristics:

```java
// For BibTeX-style formats
public class MyImporter extends BibtexImporter {
    // Implementation
}

// For XML-based formats
public class MyImporter extends XmlImporter {
    // Implementation
}

// For custom parsing logic
public class MyImporter implements Importer {
    // Full implementation
}
```

## Implementation Process

### 1. Format Analysis

- Study format specification and examples
- Identify required vs optional fields
- Understand encoding requirements
- Document format limitations and edge cases

### 2. Parser Implementation

Implement core parsing methods:

```java
@Override
public String getName() {
    return "My Format";
}

@Override
public FileType getFileType() {
    return FileType.fromExtension("myext");
}

@Override
public List<BibEntry> importEntries(InputStream stream, Charset encoding) throws ImportException {
    // Parse logic here
}
```

### 3. Field Mapping

Map format-specific fields to JabRef's internal model:

```java
private void mapFields(BibEntry entry, ParsedData data) {
    // Standard mappings
    setFieldIfPresent(entry, StandardField.TITLE, data.getTitle());
    setFieldIfPresent(entry, StandardField.AUTHOR, data.getAuthors());

    // Format-specific mappings
    setFieldIfPresent(entry, FieldName.DOI, data.getDoi());
    setFieldIfPresent(entry, FieldName.ISSN, data.getIssn());
}
```

## Format Detection

### File Extension Detection

```java
@Override
public FileType getFileType() {
    return new FileType("My Format", "myext");
}
```

### Content-Based Detection

For formats without specific extensions:

```java
@Override
public boolean isRecognizedFormat(InputStream stream) throws IOException {
    // Read first few bytes
    byte[] buffer = new byte[512];
    int bytesRead = stream.read(buffer);

    // Check for format signatures
    String content = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
    return content.contains("MY_FORMAT_IDENTIFIER");
}
```

## Parsing Strategies

### Line-Based Parsing

For simple formats with one entry per line:

```java
private List<BibEntry> parseLineBased(BufferedReader reader) throws IOException {
    List<BibEntry> entries = new ArrayList<>();
    String line;

    while ((line = reader.readLine()) != null) {
        if (isEntryLine(line)) {
            BibEntry entry = parseEntry(line);
            entries.add(entry);
        }
    }

    return entries;
}
```

### Block-Based Parsing

For formats with multi-line entries:

```java
private List<BibEntry> parseBlockBased(BufferedReader reader) throws IOException {
    List<BibEntry> entries = new ArrayList<>();
    StringBuilder currentBlock = new StringBuilder();

    String line;
    while ((line = reader.readLine()) != null) {
        if (isEntryStart(line)) {
            // Process previous block if exists
            if (currentBlock.length() > 0) {
                BibEntry entry = parseBlock(currentBlock.toString());
                entries.add(entry);
                currentBlock = new StringBuilder();
            }
        }
        currentBlock.append(line).append('\n');
    }

    // Process final block
    if (currentBlock.length() > 0) {
        BibEntry entry = parseBlock(currentBlock.toString());
        entries.add(entry);
    }

    return entries;
}
```

### XML Parsing

For XML-based formats:

```java
private List<BibEntry> parseXml(InputStream stream) throws ImportException {
    try {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(stream);

        NodeList entryNodes = doc.getElementsByTagName("entry");
        List<BibEntry> entries = new ArrayList<>();

        for (int i = 0; i < entryNodes.getLength(); i++) {
            Element entryElement = (Element) entryNodes.item(i);
            BibEntry entry = parseXmlEntry(entryElement);
            entries.add(entry);
        }

        return entries;
    } catch (Exception e) {
        throw new ImportException("XML parsing failed", e);
    }
}
```

## Data Validation and Cleanup

### Field Validation

```java
private void validateEntry(BibEntry entry) throws ImportException {
    // Required fields
    if (!entry.hasField(StandardField.TITLE)) {
        throw new ImportException("Missing required title field");
    }

    // Field format validation
    entry.getField(StandardField.YEAR).ifPresent(year -> {
        if (!year.matches("\\d{4}")) {
            // Attempt to clean or warn
        }
    });
}
```

### Author Name Normalization

```java
private void normalizeAuthors(BibEntry entry) {
    entry.getField(StandardField.AUTHOR).ifPresent(authors -> {
        try {
            AuthorList authorList = AuthorList.parse(authors);
            entry.setField(StandardField.AUTHOR, authorList.getAsFirstLastNamesWithAnd());
        } catch (Exception e) {
            // Keep original if parsing fails
        }
    });
}
```

### Duplicate Handling

```java
private List<BibEntry> removeDuplicates(List<BibEntry> entries) {
    Set<String> seenKeys = new HashSet<>();
    List<BibEntry> uniqueEntries = new ArrayList<>();

    for (BibEntry entry : entries) {
        String key = generateKey(entry);
        if (!seenKeys.contains(key)) {
            seenKeys.add(key);
            uniqueEntries.add(entry);
        }
    }

    return uniqueEntries;
}
```

## Error Handling

### Parse Exceptions

```java
try {
    // Parsing logic
} catch (IOException e) {
    throw new ImportException("Failed to read file", e);
} catch (ParseException e) {
    throw new ImportException("Invalid format at line " + lineNumber, e);
} catch (Exception e) {
    throw new ImportException("Unexpected error during import", e);
}
```

### Recovery Strategies

```java
private BibEntry parseEntryWithRecovery(String rawEntry) {
    try {
        return parseEntry(rawEntry);
    } catch (ParseException e) {
        // Attempt recovery
        String cleaned = cleanMalformedEntry(rawEntry);
        try {
            return parseEntry(cleaned);
        } catch (ParseException e2) {
            // Create minimal entry with available data
            return createPartialEntry(rawEntry);
        }
    }
}
```

## Testing Requirements

### Unit Tests

- Test with valid format files
- Test error conditions and malformed input
- Verify field mapping accuracy
- Check encoding handling

```java
@Test
void importValidFileReturnsExpectedEntries() throws Exception {
    List<BibEntry> entries = importer.importEntries(getTestFile("valid.myext"));

    assertEquals(2, entries.size());
    assertEquals("Expected Title", entries.get(0).getTitle());
}
```

### Integration Tests

- Test with real-world files
- Verify import through JabRef UI
- Check database integration
- Validate export/import roundtrips

### Test Data

- Include sample files in `src/test/resources`
- Cover edge cases (empty files, malformed entries)
- Test various encodings (UTF-8, ISO-8859-1, etc.)
- Include files with special characters

## Performance Considerations

### Memory Management

```java
// For large files, process in chunks
private List<BibEntry> parseLargeFile(InputStream stream) throws IOException {
    List<BibEntry> entries = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, encoding))) {
        String line;
        while ((line = reader.readLine()) != null) {
            // Process line immediately rather than accumulating
            BibEntry entry = parseLine(line);
            if (entry != null) {
                entries.add(entry);
            }
        }
    }
    return entries;
}
```

### Streaming Parsing

For very large files, consider streaming approaches to avoid loading entire file into memory.

## Registration and Discovery

### Importer Registration

Add to import format registry:

```java
// In ImportFormatReader or similar
private static final List<Importer> IMPORTERS = List.of(
    // ... existing importers
    new MyImporter()
);
```

### File Type Association

Ensure file extensions are properly associated in the UI.

## Code Quality Standards

### Naming Conventions

- Class name: `[Format]Importer.java`
- Test class: `[Format]ImporterTest.java`
- Constants: descriptive names for format-specific values

### Documentation

- Javadoc for all public methods
- Comments for complex parsing logic
- Reference to format specification
- Document known limitations

### Error Messages

- Provide clear, actionable error messages
- Include context (line numbers, field names)
- Suggest possible fixes when applicable

## Maintenance Guidelines

### Format Updates

- Monitor format specification changes
- Update parser when format evolves
- Maintain backward compatibility
- Document version support

### Bug Fixes

- Add regression tests for fixed issues
- Update test files to cover bug scenarios
- Document workarounds for format quirks

### Deprecation

- Mark deprecated formats with warnings
- Provide migration guidance
- Remove after transition period
- Update user documentation

## Common Patterns by Format Type

### BibTeX Variants

- Extend `BibtexImporter` for similar formats
- Handle dialect differences in field parsing
- Preserve original formatting when possible

### XML Formats

- Use standard XML parsing libraries
- Handle namespaces appropriately
- Validate against schema if available

### Plain Text Formats

- Implement custom parsing logic
- Handle various line endings
- Support different field delimiters

### Custom Formats

- Study format thoroughly before implementation
- Consider creating custom parser framework
- Document format quirks and limitations
