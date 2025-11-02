# HTTP Server Endpoints

This guide outlines the process for implementing HTTP server endpoints in JabRef, including REST API design, request handling, and response formatting.

## Server Architecture

JabRef's HTTP server provides RESTful APIs for bibliography management:

### API Categories
- **Bibliography Management**: CRUD operations on bibliography entries
- **Search and Query**: Advanced search and filtering capabilities
- **Import/Export**: Format conversion and data transfer
- **Metadata Services**: Citation generation and validation

### Server Components
- **Request Router**: Route incoming HTTP requests to handlers
- **Request Handler**: Process requests and generate responses
- **Data Serializer**: Convert between JSON and internal data models
- **Authentication**: Handle user authentication and authorization

## Implementation Process

### 1. Endpoint Planning
- Define the API purpose and target clients
- Design RESTful URL structure and HTTP methods
- Specify request/response formats (JSON schemas)
- Plan authentication and authorization requirements

### 2. Handler Implementation
Create request handlers following REST conventions:

```java
@Path("/api/entries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EntryResource {

    private final BibDatabase database;
    private final ObjectMapper objectMapper;

    @GET
    public Response getEntries(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("search") String searchQuery) {

        try {
            List<BibEntry> entries = database.getEntries();

            // Apply search filter if provided
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                entries = filterEntries(entries, searchQuery);
            }

            // Apply pagination
            List<BibEntry> paginatedEntries = applyPagination(entries, offset, limit);

            // Convert to JSON
            List<EntryDTO> dtos = paginatedEntries.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            EntryListResponse response = new EntryListResponse(
                dtos,
                entries.size(),
                offset,
                limit
            );

            return Response.ok(response).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve entries", e.getMessage()))
                    .build();
        }
    }

    @POST
    public Response createEntry(CreateEntryRequest request) {
        try {
            // Validate request
            validateCreateRequest(request);

            // Convert to BibEntry
            BibEntry entry = convertFromDTO(request);

            // Generate citation key if not provided
            if (entry.getCitationKey().isEmpty()) {
                String citationKey = CitationKeyGenerator.generateCitationKey(entry, database);
                entry.setCitationKey(citationKey);
            }

            // Add to database
            database.insertEntry(entry);

            // Return created entry
            EntryDTO responseDTO = convertToDTO(entry);
            return Response.status(Response.Status.CREATED)
                    .entity(responseDTO)
                    .build();

        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation failed", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create entry", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getEntry(@PathParam("id") String id) {
        try {
            BibEntry entry = findEntryById(id);
            if (entry == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Entry not found", "No entry with ID: " + id))
                        .build();
            }

            EntryDTO dto = convertToDTO(entry);
            return Response.ok(dto).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve entry", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateEntry(@PathParam("id") String id, UpdateEntryRequest request) {
        try {
            BibEntry existingEntry = findEntryById(id);
            if (existingEntry == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Entry not found", "No entry with ID: " + id))
                        .build();
            }

            // Validate request
            validateUpdateRequest(request);

            // Update entry
            updateEntryFromDTO(existingEntry, request);

            EntryDTO responseDTO = convertToDTO(existingEntry);
            return Response.ok(responseDTO).build();

        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation failed", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to update entry", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteEntry(@PathParam("id") String id) {
        try {
            BibEntry entry = findEntryById(id);
            if (entry == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Entry not found", "No entry with ID: " + id))
                        .build();
            }

            database.removeEntry(entry);
            return Response.noContent().build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete entry", e.getMessage()))
                    .build();
        }
    }
}
```

### 3. Data Transfer Objects
Define DTOs for request/response serialization:

```java
// Request DTOs
public class CreateEntryRequest {
    @NotNull
    private String entryType;

    @NotNull
    private Map<String, String> fields;

    private String citationKey;

    // Getters and setters
}

public class UpdateEntryRequest {
    private Map<String, String> fields;
    private String citationKey;

    // Getters and setters
}

// Response DTOs
public class EntryDTO {
    private String id;
    private String citationKey;
    private String entryType;
    private Map<String, String> fields;
    private Instant created;
    private Instant modified;

    // Getters and setters
}

public class EntryListResponse {
    private List<EntryDTO> entries;
    private int totalCount;
    private int offset;
    private int limit;

    // Getters and setters
}

public class ErrorResponse {
    private String error;
    private String message;
    private String details;

    // Constructors and getters
}
```

## Request Processing Patterns

### Input Validation
```java
public class RequestValidator {
    public static void validateCreateEntryRequest(CreateEntryRequest request) {
        if (request.getEntryType() == null || request.getEntryType().trim().isEmpty()) {
            throw new ValidationException("Entry type is required");
        }

        if (request.getFields() == null || request.getFields().isEmpty()) {
            throw new ValidationException("At least one field is required");
        }

        // Validate entry type exists
        if (!EntryTypeFactory.isValidEntryType(request.getEntryType())) {
            throw new ValidationException("Invalid entry type: " + request.getEntryType());
        }

        // Validate required fields for entry type
        EntryType entryType = EntryTypeFactory.parse(request.getEntryType());
        List<Field> requiredFields = entryType.getRequiredFields();

        for (Field requiredField : requiredFields) {
            if (!request.getFields().containsKey(requiredField.getName())) {
                throw new ValidationException("Required field missing: " + requiredField.getName());
            }
        }
    }

    public static void validateEntryId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("Entry ID is required");
        }

        // Validate ID format (assuming UUIDs)
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid entry ID format");
        }
    }
}
```

### Search and Filtering
```java
@GET
@Path("/search")
public Response searchEntries(
        @QueryParam("q") String query,
        @QueryParam("type") String entryType,
        @QueryParam("author") String author,
        @QueryParam("year") String year,
        @QueryParam("offset") @DefaultValue("0") int offset,
        @QueryParam("limit") @DefaultValue("50") int limit) {

    try {
        List<BibEntry> allEntries = database.getEntries();
        Stream<BibEntry> filteredStream = allEntries.stream();

        // Apply text search
        if (query != null && !query.trim().isEmpty()) {
            filteredStream = filteredStream.filter(entry ->
                matchesQuery(entry, query));
        }

        // Apply filters
        if (entryType != null) {
            filteredStream = filteredStream.filter(entry ->
                entryType.equals(entry.getType().getName()));
        }

        if (author != null) {
            filteredStream = filteredStream.filter(entry ->
                containsAuthor(entry, author));
        }

        if (year != null) {
            filteredStream = filteredStream.filter(entry ->
                year.equals(entry.getField(StandardField.YEAR).orElse("")));
        }

        // Apply pagination
        List<BibEntry> results = filteredStream
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        List<EntryDTO> dtos = results.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        SearchResponse response = new SearchResponse(dtos, allEntries.size());
        return Response.ok(response).build();

    } catch (Exception e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Search failed", e.getMessage()))
                .build();
    }
}

private boolean matchesQuery(BibEntry entry, String query) {
    String lowerQuery = query.toLowerCase();

    // Search in title
    if (entry.getTitle().orElse("").toLowerCase().contains(lowerQuery)) {
        return true;
    }

    // Search in author
    if (entry.getAuthor().orElse("").toLowerCase().contains(lowerQuery)) {
        return true;
    }

    // Search in abstract
    if (entry.getField(StandardField.ABSTRACT).orElse("").toLowerCase().contains(lowerQuery)) {
        return true;
    }

    return false;
}
```

## Response Formatting

### JSON Serialization
```java
public class JsonConverter {
    private final ObjectMapper objectMapper;

    public JsonConverter() {
        this.objectMapper = new ObjectMapper();
        // Configure date formatting
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        // Configure serialization features
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public EntryDTO convertToDTO(BibEntry entry) {
        EntryDTO dto = new EntryDTO();
        dto.setId(entry.getId());
        dto.setCitationKey(entry.getCitationKey().orElse(null));
        dto.setEntryType(entry.getType().getName());

        // Convert fields
        Map<String, String> fields = new HashMap<>();
        for (Field field : entry.getFields()) {
            entry.getField(field).ifPresent(value -> fields.put(field.getName(), value));
        }
        dto.setFields(fields);

        return dto;
    }

    public BibEntry convertFromDTO(CreateEntryRequest request) {
        EntryType entryType = EntryTypeFactory.parse(request.getEntryType());
        BibEntry entry = new BibEntry(entryType);

        // Set citation key if provided
        if (request.getCitationKey() != null) {
            entry.setCitationKey(request.getCitationKey());
        }

        // Set fields
        for (Map.Entry<String, String> fieldEntry : request.getFields().entrySet()) {
            Field field = FieldFactory.parseField(fieldEntry.getKey());
            entry.setField(field, fieldEntry.getValue());
        }

        return entry;
    }
}
```

### Content Negotiation
```java
@GET
@Path("/export")
@Produces({MediaType.APPLICATION_JSON, "application/x-bibtex", "application/x-endnote"})
public Response exportEntries(
        @QueryParam("format") @DefaultValue("json") String format,
        @QueryParam("ids") List<String> entryIds) {

    try {
        List<BibEntry> entries = getEntriesByIds(entryIds);

        switch (format.toLowerCase()) {
            case "bibtex":
                return exportAsBibTeX(entries);
            case "endnote":
                return exportAsEndNote(entries);
            case "json":
            default:
                return exportAsJson(entries);
        }

    } catch (Exception e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Export failed", e.getMessage()))
                .build();
    }
}

private Response exportAsBibTeX(List<BibEntry> entries) throws IOException {
    BibtexExporter exporter = new BibtexExporter();
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    exporter.export(new BibDatabase(entries), output, StandardCharsets.UTF_8, entries);

    return Response.ok(output.toString(StandardCharsets.UTF_8))
            .header("Content-Disposition", "attachment; filename=\"export.bib\"")
            .build();
}

private Response exportAsJson(List<BibEntry> entries) {
    List<EntryDTO> dtos = entries.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

    return Response.ok(dtos)
            .header("Content-Disposition", "attachment; filename=\"export.json\"")
            .build();
}
```

## Error Handling

### Global Exception Handling
```java
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        // Log the exception
        logger.error("Unhandled exception in REST API", exception);

        // Determine appropriate status code
        Response.Status status;
        if (exception instanceof ValidationException) {
            status = Response.Status.BAD_REQUEST;
        } else if (exception instanceof NotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (exception instanceof SecurityException) {
            status = Response.Status.FORBIDDEN;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        // Create error response
        ErrorResponse errorResponse = new ErrorResponse(
            "An error occurred",
            exception.getMessage(),
            getStackTrace(exception)
        );

        return Response.status(status)
                .entity(errorResponse)
                .build();
    }

    private String getStackTrace(Throwable throwable) {
        // Only include stack trace in development mode
        if (isDevelopmentMode()) {
            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }
        return null;
    }
}
```

### Validation Exception Handling
```java
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
            "Validation failed",
            exception.getMessage(),
            null // Don't expose internal details
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }
}
```

## Authentication and Authorization

### Basic Authentication
```java
@Provider
@Priority(Priorities.AUTHENTICATION)
public class BasicAuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = requestContext.getHeaderString("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"JabRef API\"")
                    .build());
            return;
        }

        // Decode credentials
        String credentials = authHeader.substring("Basic ".length());
        String decoded = new String(Base64.getDecoder().decode(credentials));
        String[] parts = decoded.split(":", 2);

        if (parts.length != 2) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        String username = parts[0];
        String password = parts[1];

        // Validate credentials
        if (!authenticateUser(username, password)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        // Set security context
        requestContext.setSecurityContext(new JabRefSecurityContext(username));
    }
}
```

### CORS Support
```java
@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                      ContainerResponseContext responseContext) throws IOException {

        // Add CORS headers
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        headers.add("Access-Control-Allow-Credentials", "true");

        // Handle preflight requests
        if ("OPTIONS".equals(requestContext.getMethod())) {
            responseContext.setStatus(200);
        }
    }
}
```

## Testing REST Endpoints

### Unit Tests
```java
@Test
void getEntriesReturnsCorrectResponse() throws Exception {
    // Setup mock database
    BibDatabase mockDatabase = mock(BibDatabase.class);
    List<BibEntry> mockEntries = createMockEntries();
    when(mockDatabase.getEntries()).thenReturn(mockEntries);

    // Create resource
    EntryResource resource = new EntryResource(mockDatabase, new ObjectMapper());

    // Call endpoint
    Response response = resource.getEntries(0, 10, null);

    // Verify response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    EntryListResponse entity = (EntryListResponse) response.getEntity();
    assertEquals(2, entity.getEntries().size());
}
```

### Integration Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EntryResourceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createEntryIntegrationTest() {
        CreateEntryRequest request = new CreateEntryRequest();
        request.setEntryType("article");
        request.setFields(Map.of(
            "title", "Test Title",
            "author", "Test Author",
            "year", "2023"
        ));

        ResponseEntity<EntryDTO> response = restTemplate.postForEntity(
            "/api/entries", request, EntryDTO.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
        assertEquals("Test Title", response.getBody().getFields().get("title"));
    }
}
```

## Performance Optimization

### Caching
```java
@GET
@Path("/entries/{id}")
@Cacheable(cacheName = "entries", key = "#id")
public Response getEntry(@PathParam("id") String id) {
    // Implementation
}

@POST
@CacheEvict(cacheName = "entries", allEntries = true)
public Response createEntry(CreateEntryRequest request) {
    // Implementation
}
```

### Asynchronous Processing
```java
@POST
@Path("/import")
public CompletionStage<Response> importEntriesAsync(ImportRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            List<BibEntry> entries = performImport(request);
            return Response.ok(new ImportResponse(entries.size())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Import failed", e.getMessage()))
                    .build();
        }
    });
}
```

## API Documentation

### OpenAPI Specification
```java
@OpenAPIDefinition(
    info = @Info(
        title = "JabRef REST API",
        version = "1.0.0",
        description = "REST API for bibliography management"
    ),
    servers = @Server(url = "/api")
)
public class ApiDocumentation {
    // This class can be empty - annotations provide the documentation
}
```

### Endpoint Documentation
```java
@GET
@Operation(
    summary = "Get bibliography entries",
    description = "Retrieve a paginated list of bibliography entries with optional search"
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Successful operation",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = EntryListResponse.class))),
    @ApiResponse(responseCode = "500", description = "Internal server error")
})
public Response getEntries(
        @Parameter(description = "Pagination offset") @QueryParam("offset") int offset,
        @Parameter(description = "Maximum number of results") @QueryParam("limit") int limit,
        @Parameter(description = "Search query") @QueryParam("search") String searchQuery) {
    // Implementation
}
```

## Maintenance Guidelines

### API Versioning
- Use URL versioning: `/api/v1/entries`
- Maintain backward compatibility within major versions
- Deprecate old versions with clear migration paths
- Document breaking changes in release notes

### Monitoring and Logging
- Log all API requests and responses
- Monitor response times and error rates
- Track API usage patterns
- Alert on performance degradation

### Security Updates
- Regularly update dependencies
- Implement rate limiting
- Validate all input data
- Use HTTPS in production
- Implement proper authentication
