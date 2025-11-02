# Implementing Bibliography Export Formats

This guide outlines the process for implementing new bibliography export formats in JabRef, including serialization strategies, configuration options, and validation.

## Exporter Types and Architecture

JabRef supports multiple export formats through a flexible exporter architecture:

### Format Categories

- **BibTeX/BibLaTeX**: Native JabRef formats with variants
- **XML-based**: MODS, MARC, EndNote XML
- **Plain text**: RIS, ISI, custom delimited
- **Specialized**: HTML, RTF, custom publisher formats

### Base Classes

Choose appropriate base class based on export requirements:

```java
// For BibTeX-style exports
public class MyExporter extends BibtexExporter {
    // Implementation
}

// For template-based exports
public class MyExporter extends TemplateExporter {
    // Implementation
}

// For custom export logic
public class MyExporter implements Exporter {
    // Full implementation
}
```

## Implementation Process

### 1. Format Specification

- Study target format requirements and specifications
- Identify supported fields and their mappings
- Understand encoding and formatting requirements
- Document format limitations and constraints

### 2. Exporter Implementation

Implement core export methods:

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
public void export(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries) throws Exception {
    // Export logic here
}
```

### 3. Field Mapping and Serialization

Map JabRef fields to format-specific representations:

```java
private void writeEntry(BibEntry entry, Writer writer) throws IOException {
    // Format-specific serialization
    writer.write("@entry{");
    writer.write(entry.getCitationKey().orElse("unknown"));
    writer.write(",\n");

    // Map fields
    writeField(writer, "title", entry.getTitle().orElse(""));
    writeField(writer, "author", entry.getAuthor().orElse(""));

    writer.write("}\n");
}
```

## Export Strategies

### Template-Based Export

For formats with consistent structure:

```java
public class MyTemplateExporter extends TemplateExporter {

    @Override
    public String getName() {
        return "My Template Format";
    }

    @Override
    protected String getTemplateName() {
        return "my-template.vm"; // Velocity template
    }
}
```

### Custom Serialization

For complex or custom formats:

```java
private void exportCustomFormat(List<BibEntry> entries, Writer writer) throws IOException {
    // Custom serialization logic
    for (BibEntry entry : entries) {
        writeCustomEntry(entry, writer);
        writer.write("\n");
    }
}
```

### XML Export

For XML-based formats:

```java
private void exportXml(List<BibEntry> entries, Writer writer) throws IOException {
    try {
        Document doc = createXmlDocument(entries);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
    } catch (Exception e) {
        throw new IOException("XML export failed", e);
    }
}
```

## Configuration and Options

### Export Preferences

Handle format-specific configuration:

```java
public class MyExporterPreferences {
    private final boolean includeAbstract;
    private final boolean useShortTitles;
    private final String customDelimiter;

    // Constructor and getters
}
```

### Save Configuration

Implement save configuration for advanced options:

```java
@Override
public SaveConfiguration getSaveConfiguration() {
    return new SelfContainedSaveConfiguration(
        BibDatabaseMode.BIBTEX,
        true,  // reformat
        true,  // sort
        new MSBibExportFormatTestFiles(), // encoding
        false  // make backups
    );
}
```

## Field Mapping and Transformation

### Standard Field Mapping

```java
private String mapField(BibEntry entry, StandardField field) {
    return entry.getField(field)
            .map(this::transformFieldValue)
            .orElse("");
}
```

### Custom Field Handling

```java
private void handleCustomFields(BibEntry entry, Writer writer) throws IOException {
    for (Field field : entry.getFields()) {
        if (isCustomField(field)) {
            writeCustomField(writer, field, entry.getField(field).orElse(""));
        }
    }
}
```

### Data Transformation

```java
private String transformAuthor(String author) {
    try {
        AuthorList authorList = AuthorList.parse(author);
        return authorList.getAsLastFirstNamesWithAnd();
    } catch (Exception e) {
        return author; // Return original if parsing fails
    }
}
```

## Error Handling and Validation

### Export Validation

```java
private void validateEntries(List<BibEntry> entries) throws ExportException {
    for (BibEntry entry : entries) {
        if (!entry.hasField(StandardField.TITLE)) {
            throw new ExportException("Entry missing required title field");
        }
    }
}
```

### Graceful Degradation

```java
private String safeGetField(BibEntry entry, Field field) {
    try {
        return entry.getField(field).orElse("");
    } catch (Exception e) {
        // Log warning and return empty
        return "";
    }
}
```

## Encoding and Character Handling

### Character Encoding

```java
@Override
public void export(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries) throws Exception {
    try (BufferedWriter writer = Files.newBufferedWriter(file, encoding)) {
        // Export with specified encoding
        exportEntries(entries, writer);
    }
}
```

### Special Character Handling

```java
private String escapeSpecialChars(String value) {
    return value.replace("&", "&")
                .replace("<", "<")
                .replace(">", ">")
                .replace("\"", """);
}
```

## Performance Considerations

### Batch Processing

```java
private void exportInBatches(List<BibEntry> entries, Writer writer, int batchSize) throws IOException {
    for (int i = 0; i < entries.size(); i += batchSize) {
        int endIndex = Math.min(i + batchSize, entries.size());
        List<BibEntry> batch = entries.subList(i, endIndex);
        exportBatch(batch, writer);
    }
}
```

### Memory Management

For large exports, consider streaming approaches to avoid loading all data into memory simultaneously.

## Testing Requirements

### Unit Tests

- Test export of individual entries
- Verify field mapping and transformation
- Check error handling and edge cases
- Validate output format compliance

```java
@Test
void exportSingleEntryProducesValidOutput() throws Exception {
    BibEntry entry = createTestEntry();
    String output = exportToString(List.of(entry));

    assertTrue(output.contains("expected content"));
    assertTrue(isValidFormat(output));
}
```

### Integration Tests

- Test full database export
- Verify file creation and encoding
- Check import/export roundtrip compatibility
- Validate with external tools

### Test Data

- Use diverse bibliography entries
- Include special characters and edge cases
- Test with various field combinations
- Verify output against format specifications

## Registration and Discovery

### Exporter Registration

Add to export factory:

```java
// In ExporterFactory or similar
private static final List<Exporter> EXPORTERS = List.of(
    // ... existing exporters
    new MyExporter()
);
```

### UI Integration

Ensure exporter appears in export dialogs and menus.

## Code Quality Standards

### Naming Conventions

- Class name: `[Format]Exporter.java`
- Test class: `[Format]ExporterTest.java`
- Methods: descriptive action-oriented names

### Documentation

- Javadoc for all public methods
- Comments for complex transformation logic
- Reference to format specification
- Document known limitations

### Error Messages

- Provide clear, descriptive error messages
- Include context about what failed
- Suggest possible solutions

## Maintenance Guidelines

### Format Updates

- Monitor format specification changes
- Update exporter when format evolves
- Maintain backward compatibility
- Document version support

### Bug Fixes

- Add regression tests for fixed issues
- Update test expectations
- Document workarounds for format quirks

### Deprecation

- Mark deprecated exporters with warnings
- Provide migration guidance
- Remove after transition period
- Update user documentation

## Common Patterns by Format Type

### BibTeX Variants

- Extend `BibtexExporter` for similar formats
- Handle dialect differences in field output
- Preserve JabRef-specific features when possible

### XML Formats

- Use standard XML generation libraries
- Include proper XML declarations and encoding
- Validate against schema if available

### Plain Text Formats

- Implement custom formatting logic
- Handle line endings and field delimiters
- Support various output encodings

### Template-Based Formats

- Use Velocity or similar templating engines
- Separate template files from Java code
- Allow template customization

## Quality Assurance

### Output Validation

```java
private boolean isValidFormat(String output) {
    // Format-specific validation
    return output.startsWith("expected header") &&
           output.contains("required elements");
}
```

### Consistency Checks

- Verify exported files can be re-imported
- Check field preservation across export/import
- Validate against reference implementations
