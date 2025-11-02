# Implementing Bibliographic Data Fetchers

This guide outlines the process for implementing new bibliographic data fetchers in JabRef, including API integration, error handling, and testing patterns.

## Fetcher Types

JabRef supports several types of fetchers, each with specific use cases:

### EntryBasedFetcher

- **Purpose**: Enhance existing bibliography entries with additional data
- **Use Case**: Add missing fields (DOI, ISBN, abstract) to existing entries
- **Example**: DOI resolution, ISBN lookup

### SearchBasedFetcher

- **Purpose**: Search external databases and return new bibliography entries
- **Use Case**: Import entries from search queries
- **Example**: Google Scholar, DBLP search

### FulltextFetcher

- **Purpose**: Find PDF links for existing bibliography entries
- **Use Case**: Locate full-text documents
- **Example**: DOI-to-PDF resolution, publisher PDF links

### IdBasedFetcher

- **Purpose**: Fetch entries using unique identifiers
- **Use Case**: Direct lookup by DOI, ISBN, or other IDs
- **Example**: CrossRef DOI lookup, ISBN database search

## Implementation Process

### 1. Planning

- Identify the target service and its API
- Determine fetcher type based on use case
- Research API requirements (keys, rate limits, terms of service)
- Check existing similar fetchers for patterns

### 2. Base Class Selection

Choose appropriate base class based on fetcher type:

```java
// For ID-based fetching
public class MyFetcher extends AbstractIsbnFetcher {
    // Implementation
}

// For search-based fetching
public class MyFetcher extends AbstractSearchBasedFetcher {
    // Implementation
}

// For fulltext fetching
public class MyFetcher implements FulltextFetcher {
    // Implementation
}
```

### 3. Core Implementation

Implement required methods based on fetcher type:

```java
@Override
public String getName() {
    return "My Service";
}

@Override
protected URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
    // Build API URL
}

@Override
protected void doPostCleanup(BibEntry entry) {
    // Clean up fetched data
}
```

## API Integration Patterns

### Authentication

Handle different authentication methods:

```java
// API Key from preferences
private String getApiKey() {
    return importerPreferences.getApiKey(FETCHER_NAME);
}

// Environment variable fallback
private String getApiKey() {
    String key = System.getenv("MY_API_KEY");
    if (key == null) {
        key = importerPreferences.getApiKey(FETCHER_NAME);
    }
    return key;
}
```

### Rate Limiting

Implement appropriate rate limiting:

```java
private static final int RATE_LIMIT_DELAY_MS = 1000; // 1 second between requests

private void enforceRateLimit() {
    try {
        Thread.sleep(RATE_LIMIT_DELAY_MS);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

### Error Handling

Use JabRef's exception hierarchy:

```java
try {
    // API call
} catch (HttpClientException e) {
    throw new FetcherClientException("Service unavailable", e);
} catch (HttpServerException e) {
    throw new FetcherServerException("Server error", e);
} catch (Exception e) {
    throw new FetcherException("Unexpected error", e);
}
```

## Trust Levels for Fulltext Fetchers

Assign appropriate trust levels based on source reliability:

```java
@Override
public TrustLevel getTrustLevel() {
    return TrustLevel.PUBLISHER; // or SOURCE, PREPRINT, META_SEARCH
}
```

### Trust Level Guidelines

- **SOURCE**: Definitive source (DOI resolution)
- **PUBLISHER**: Official publisher sites
- **PREPRINT**: Preprint servers (arXiv, etc.)
- **META_SEARCH**: Search aggregators

## Testing Requirements

### Unit Tests

- Mock HTTP responses using `HttpClient`
- Test error conditions and edge cases
- Verify data parsing and cleanup
- Check rate limiting behavior

```java
@Test
void successfulFetchReturnsExpectedEntry() throws Exception {
    // Mock HTTP response
    mockHttpResponse("expected-response.json");

    Optional<BibEntry> result = fetcher.performSearch("test query");

    assertTrue(result.isPresent());
    assertEquals("Expected Title", result.get().getTitle());
}
```

### Integration Tests

- Test with real APIs (marked with `@FetcherTest`)
- Verify API key handling
- Test rate limit behavior
- Validate error responses

### Test Data

- Use realistic test data
- Include edge cases (missing fields, malformed responses)
- Test with various character encodings
- Verify field mapping accuracy

## Configuration and Preferences

### API Keys

Register fetcher in preferences system:

```java
// In JabRefCliPreferences or similar
keys.put(MyFetcher.FETCHER_NAME, buildInfo.myApiKey);
```

### Environment Variables

Document required environment variables in fetchers.md:

```markdown
| Service | Key Source | Environment Variable | Rate Limit |
|---------|------------|---------------------|------------|
| My Service | API Portal | `MyApiKey` | 1000/day |
```

## Registration and Discovery

### WebFetchers Registration

Add to `WebFetchers.java`:

```java
private static final List<Fetcher> FETCHERS = List.of(
    // ... existing fetchers
    new MyFetcher()
);
```

### Preferences Integration

Ensure fetcher appears in preferences UI by adding to appropriate lists.

## Code Quality Standards

### Naming Conventions

- Class name: `[Service]Fetcher.java`
- Constants: `FETCHER_NAME = "My Service"`
- Methods: descriptive, action-oriented names

### Documentation

- Javadoc for all public methods
- Comments for complex logic
- Reference API documentation
- Document limitations and assumptions

### Performance

- Minimize HTTP requests
- Cache responses when appropriate
- Handle timeouts gracefully
- Avoid blocking operations

## Common Patterns

### Response Parsing

```java
private BibEntry parseResponse(String response) throws FetcherException {
    try {
        JsonNode root = objectMapper.readTree(response);
        BibEntry entry = new BibEntry();

        // Extract fields
        Optional.ofNullable(root.get("title"))
                .map(JsonNode::asText)
                .ifPresent(entry::setTitle);

        return entry;
    } catch (JsonProcessingException e) {
        throw new FetcherException("Failed to parse response", e);
    }
}
```

### Field Mapping

```java
private void mapFields(BibEntry entry, JsonNode data) {
    // Standard BibTeX fields
    setFieldIfPresent(entry, StandardField.TITLE, data.get("title"));
    setFieldIfPresent(entry, StandardField.AUTHOR, data.get("authors"));
    setFieldIfPresent(entry, StandardField.YEAR, data.get("year"));

    // Custom fields
    setFieldIfPresent(entry, FieldName.DOI, data.get("doi"));
    setFieldIfPresent(entry, FieldName.ISBN, data.get("isbn"));
}
```

### Cleanup Operations

```java
@Override
protected void doPostCleanup(BibEntry entry) {
    // Normalize author names
    entry.getField(StandardField.AUTHOR).ifPresent(authors ->
        entry.setField(StandardField.AUTHOR, AuthorList.parse(authors).getAsFirstLastNamesWithAnd())
    );

    // Clean up URLs
    entry.getField(StandardField.URL).ifPresent(url ->
        entry.setField(StandardField.URL, url.trim())
    );
}
```

## Maintenance Guidelines

### API Changes

- Monitor API documentation for changes
- Update implementation when APIs evolve
- Maintain backward compatibility when possible
- Document API version requirements

### Deprecation

- Mark deprecated fetchers with `@Deprecated`
- Provide migration guidance
- Remove after reasonable transition period
- Update documentation and preferences

### Monitoring

- Track fetcher usage and success rates
- Monitor API rate limit consumption
- Log errors for debugging
- Update contact information for API providers
