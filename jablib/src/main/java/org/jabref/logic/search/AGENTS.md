# Search Functionality

This guide outlines the process for implementing search functionality in JabRef, including indexing, querying, and result retrieval.

## Search Architecture

JabRef's search system consists of multiple layers:

### Indexing Layer
- **Document Indexing**: Convert bibliography entries to searchable documents
- **Field Mapping**: Map BibTeX fields to search fields
- **Incremental Updates**: Update index as entries change

### Query Layer
- **Query Parsing**: Parse user search strings into query objects
- **Query Optimization**: Optimize queries for performance
- **Query Expansion**: Expand queries with synonyms and related terms

### Retrieval Layer
- **Result Ranking**: Rank search results by relevance
- **Result Filtering**: Filter results based on criteria
- **Result Presentation**: Format results for display

## Implementation Process

### 1. Search Planning
- Define search requirements and scope
- Identify searchable fields and data types
- Plan indexing strategy and storage
- Design query syntax and features

### 2. Indexer Implementation
Create document indexer for bibliography entries:

```java
public class BibEntryIndexer {
    private final IndexWriter indexWriter;

    public void indexEntry(BibEntry entry) throws IOException {
        Document document = createDocument(entry);
        indexWriter.addDocument(document);
    }

    private Document createDocument(BibEntry entry) {
        Document document = new Document();

        // Add entry ID
        document.add(new StringField("id", entry.getId(), Field.Store.YES));

        // Add citation key
        entry.getCitationKey().ifPresent(key ->
            document.add(new StringField("citationKey", key, Field.Store.YES))
        );

        // Add searchable fields
        addSearchableField(document, "title", entry.getTitle().orElse(""));
        addSearchableField(document, "author", entry.getAuthor().orElse(""));
        addSearchableField(document, "abstract", entry.getField(StandardField.ABSTRACT).orElse(""));

        return document;
    }

    private void addSearchableField(Document document, String fieldName, String value) {
        document.add(new TextField(fieldName, value, Field.Store.NO));
        document.add(new StringField(fieldName + "_exact", value, Field.Store.NO));
    }
}
```

### 3. Query Processor Implementation
Implement query parsing and execution:

```java
public class SearchQueryProcessor {
    private final IndexSearcher indexSearcher;

    public List<SearchResult> search(String queryString) throws IOException {
        Query query = parseQuery(queryString);
        TopDocs topDocs = indexSearcher.search(query, 100);

        return Arrays.stream(topDocs.scoreDocs)
                .map(scoreDoc -> createSearchResult(scoreDoc))
                .collect(Collectors.toList());
    }

    private Query parseQuery(String queryString) {
        // Parse query string into Lucene query
        QueryParser parser = new QueryParser("content", new StandardAnalyzer());
        try {
            return parser.parse(queryString);
        } catch (ParseException e) {
            // Fallback to simple term query
            return new TermQuery(new Term("content", queryString));
        }
    }

    private SearchResult createSearchResult(ScoreDoc scoreDoc) throws IOException {
        Document document = indexSearcher.doc(scoreDoc.doc);
        String entryId = document.get("id");

        return new SearchResult(entryId, scoreDoc.score, extractHighlights(document));
    }
}
```

## Search Query Types

### Basic Text Search
```java
public class TextSearchQuery implements SearchQuery {
    private final String searchTerm;
    private final List<String> searchFields;

    @Override
    public Query toLuceneQuery() {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for (String field : searchFields) {
            Query fieldQuery = new FuzzyQuery(new Term(field, searchTerm));
            builder.add(fieldQuery, BooleanClause.Occur.SHOULD);
        }

        return builder.build();
    }
}
```

### Field-Specific Search
```java
public class FieldSearchQuery implements SearchQuery {
    private final String fieldName;
    private final String fieldValue;

    @Override
    public Query toLuceneQuery() {
        return new TermQuery(new Term(fieldName, fieldValue));
    }
}
```

### Advanced Query Syntax
```java
public class AdvancedSearchQuery implements SearchQuery {
    private final String queryString;

    @Override
    public Query toLuceneQuery() {
        try {
            QueryParser parser = new MultiFieldQueryParser(
                new String[]{"title", "author", "abstract"},
                new StandardAnalyzer()
            );
            return parser.parse(queryString);
        } catch (ParseException e) {
            throw new SearchException("Invalid query syntax: " + queryString, e);
        }
    }
}
```

## Indexing Strategies

### Full-Text Indexing
```java
public class FullTextIndexer {
    private final Analyzer analyzer = new StandardAnalyzer();

    public void indexEntry(BibEntry entry) throws IOException {
        Document document = new Document();

        // Index all searchable content
        StringBuilder fullText = new StringBuilder();
        for (Field field : entry.getFields()) {
            entry.getField(field).ifPresent(value -> {
                document.add(new TextField(field.getName(), value, Field.Store.NO));
                fullText.append(value).append(" ");
            });
        }

        // Add combined full-text field
        document.add(new TextField("fulltext", fullText.toString(), Field.Store.NO));

        indexWriter.addDocument(document);
    }
}
```

### Incremental Indexing
```java
public class IncrementalIndexer {
    private final Map<String, Long> entryVersions = new ConcurrentHashMap<>();

    public void updateEntry(BibEntry entry) throws IOException {
        String entryId = entry.getId();
        long currentVersion = getEntryVersion(entry);

        if (currentVersion > entryVersions.getOrDefault(entryId, 0L)) {
            // Entry has changed, re-index
            indexWriter.deleteDocuments(new Term("id", entryId));
            indexEntry(entry);
            entryVersions.put(entryId, currentVersion);
        }
    }

    private long getEntryVersion(BibEntry entry) {
        // Calculate version based on modification time or content hash
        return entry.getField(StandardField.MODIFICATIONDATE)
                .map(this::parseDate)
                .orElse(System.currentTimeMillis());
    }
}
```

## Result Ranking and Filtering

### Relevance Scoring
```java
public class SearchResultRanker {
    public List<SearchResult> rankResults(List<SearchResult> results, String query) {
        return results.stream()
                .sorted((a, b) -> {
                    // Custom ranking logic
                    int scoreA = calculateRelevanceScore(a, query);
                    int scoreB = calculateRelevanceScore(b, query);
                    return Integer.compare(scoreB, scoreA); // Descending order
                })
                .collect(Collectors.toList());
    }

    private int calculateRelevanceScore(SearchResult result, String query) {
        int score = (int) (result.getScore() * 100); // Base Lucene score

        // Boost for exact matches
        if (result.getTitle().toLowerCase().contains(query.toLowerCase())) {
            score += 50;
        }

        // Boost for recent entries
        if (result.isRecent()) {
            score += 25;
        }

        return score;
    }
}
```

### Result Filtering
```java
public class SearchResultFilter {
    public List<SearchResult> filterResults(List<SearchResult> results, SearchFilter filter) {
        return results.stream()
                .filter(result -> matchesFilter(result, filter))
                .collect(Collectors.toList());
    }

    private boolean matchesFilter(SearchResult result, SearchFilter filter) {
        // Apply entry type filter
        if (!filter.getEntryTypes().isEmpty() &&
            !filter.getEntryTypes().contains(result.getEntryType())) {
            return false;
        }

        // Apply date range filter
        if (filter.getDateRange() != null &&
            !filter.getDateRange().contains(result.getPublicationDate())) {
            return false;
        }

        // Apply field filters
        for (FieldFilter fieldFilter : filter.getFieldFilters()) {
            if (!matchesFieldFilter(result, fieldFilter)) {
                return false;
            }
        }

        return true;
    }
}
```

## Search Performance Optimization

### Index Optimization
```java
public class SearchIndexOptimizer {
    public void optimizeIndex() throws IOException {
        // Merge segments for better performance
        indexWriter.forceMerge(1);

        // Update index statistics
        updateIndexStatistics();
    }

    public void updateIndexStatistics() {
        // Calculate and cache index statistics
        int numDocs = indexReader.numDocs();
        long indexSize = calculateIndexSize();

        // Update performance metrics
        updatePerformanceMetrics(numDocs, indexSize);
    }
}
```

### Query Caching
```java
public class QueryCache {
    private final Cache<String, Query> queryCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public Query getOrParseQuery(String queryString) {
        return queryCache.get(queryString, () -> parseQuery(queryString));
    }

    private Query parseQuery(String queryString) {
        // Parse and optimize query
        return queryParser.parse(queryString);
    }
}
```

### Parallel Search
```java
public class ParallelSearchExecutor {
    private final ExecutorService executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    public CompletableFuture<List<SearchResult>> searchAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performSearch(query);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    private List<SearchResult> performSearch(String query) throws IOException {
        Query luceneQuery = queryProcessor.parseQuery(query);
        TopDocs topDocs = indexSearcher.search(luceneQuery, 1000);

        return Arrays.stream(topDocs.scoreDocs)
                .parallel() // Process results in parallel
                .map(scoreDoc -> createSearchResult(scoreDoc))
                .collect(Collectors.toList());
    }
}
```

## Search UI Integration

### Search Input Handling
```java
public class SearchInputHandler {
    private final ObjectProperty<String> searchQuery = new SimpleObjectProperty<>();
    private final BooleanProperty isSearching = new SimpleBooleanProperty(false);

    public SearchInputHandler() {
        // Debounce search input
        searchQuery.addListener(new DebounceChangeListener<>(300, this::performSearch));
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            clearResults();
            return;
        }

        isSearching.set(true);
        searchService.searchAsync(query)
                .thenAccept(results -> {
                    Platform.runLater(() -> {
                        updateResults(results);
                        isSearching.set(false);
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        handleSearchError(throwable);
                        isSearching.set(false);
                    });
                    return null;
                });
    }
}
```

### Results Display
```java
public class SearchResultsView {
    private final TableView<SearchResult> resultsTable;
    private final Pagination pagination;

    public SearchResultsView() {
        resultsTable = createResultsTable();
        pagination = new Pagination();

        // Bind pagination to results
        pagination.pageCountProperty().bind(
            Bindings.createIntegerBinding(
                () -> (searchResults.size() + PAGE_SIZE - 1) / PAGE_SIZE,
                searchResults.sizeProperty()
            )
        );
    }

    private TableView<SearchResult> createResultsTable() {
        TableView<SearchResult> table = new TableView<>();

        TableColumn<SearchResult, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> data.getValue().titleProperty());

        TableColumn<SearchResult, Double> scoreCol = new TableColumn<>("Relevance");
        scoreCol.setCellValueFactory(data -> data.getValue().scoreProperty().asObject());

        table.getColumns().addAll(titleCol, scoreCol);
        return table;
    }
}
```

## Testing Search Functionality

### Unit Tests
```java
@Test
void searchFindsMatchingEntries() throws IOException {
    // Setup test index
    createTestIndex();

    // Perform search
    List<SearchResult> results = searchService.search("machine learning");

    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(r -> r.getTitle().contains("learning")));
}
```

### Integration Tests
```java
@Test
void searchIntegrationTest() {
    // Create test database
    BibDatabase database = createTestDatabase();

    // Index database
    indexer.indexDatabase(database);

    // Perform search
    SearchQuery query = new TextSearchQuery("quantum computing");
    List<SearchResult> results = searcher.search(query);

    // Verify results
    assertTrue(results.size() > 0);
    assertTrue(results.get(0).getScore() > 0.5); // High relevance
}
```

## Search Analytics and Monitoring

### Usage Tracking
```java
public class SearchAnalytics {
    public void trackSearch(String query, int resultCount, long durationMs) {
        // Record search metrics
        recordSearchQuery(query);
        recordResultCount(resultCount);
        recordSearchDuration(durationMs);

        // Analyze search patterns
        analyzeSearchPatterns(query);
    }

    private void analyzeSearchPatterns(String query) {
        // Identify common search terms
        // Track failed searches
        // Suggest query improvements
    }
}
```

### Performance Monitoring
```java
public class SearchPerformanceMonitor {
    private final MeterRegistry registry;

    public void recordSearchMetrics(String query, long durationMs, int resultCount) {
        // Record metrics
        registry.timer("search.duration").record(durationMs, TimeUnit.MILLISECONDS);
        registry.counter("search.queries").increment();
        registry.gauge("search.result.count", resultCount);

        // Alert on performance issues
        if (durationMs > 5000) { // 5 seconds
            alertSlowSearch(query, durationMs);
        }
    }
}
```

## Maintenance Guidelines

### Index Maintenance
- Regularly optimize search indexes
- Monitor index size and performance
- Rebuild indexes when schema changes
- Backup and restore search indexes

### Query Optimization
- Profile slow queries and optimize
- Update query parsers for new features
- Maintain query compatibility
- Document
