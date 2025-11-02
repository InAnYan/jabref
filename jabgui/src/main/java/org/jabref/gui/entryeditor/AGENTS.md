# Entry Editor Tabs

This guide outlines the process for implementing entry editor tabs in JabRef, including tab creation, field management, and user interaction patterns.

## Entry Editor Architecture

JabRef's entry editor consists of multiple tabs that organize bibliography fields:

### Tab Categories
- **Required Fields**: Essential fields for bibliography entries
- **Optional Fields**: Additional metadata fields
- **Specialized Tabs**: AI summaries, citations, file annotations
- **Custom Tabs**: User-defined field collections

### Tab Components
- **Tab Controller**: Manages tab lifecycle and data binding
- **Field Editors**: Individual field input components
- **Validation**: Real-time field validation and error display
- **Actions**: Tab-specific actions and operations

## Implementation Process

### 1. Tab Planning
- Define the tab's purpose and target fields
- Determine layout and user interaction patterns
- Plan validation requirements and error handling
- Consider performance implications for large entries

### 2. Tab Controller Implementation
Create the main tab controller:

```java
public class RequiredFieldsTab extends EntryEditorTab {
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField yearField;
    @FXML private ComboBox<String> entryTypeCombo;

    private final RequiredFieldsTabViewModel viewModel;

    public RequiredFieldsTab() {
        this.viewModel = new RequiredFieldsTabViewModel();
    }

    @FXML
    public void initialize() {
        // Bind fields to view model
        titleField.textProperty().bindBidirectional(viewModel.titleProperty());
        authorField.textProperty().bindBidirectional(viewModel.authorProperty());
        yearField.textProperty().bindBidirectional(viewModel.yearProperty());
        entryTypeCombo.setItems(viewModel.getAvailableEntryTypes());
        entryTypeCombo.valueProperty().bindBidirectional(viewModel.entryTypeProperty());

        // Setup validation
        setupValidation();
    }

    private void setupValidation() {
        // Add validation listeners
        titleField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Lost focus
                validateTitle();
            }
        });
    }

    private void validateTitle() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) {
            showValidationError(titleField, "Title is required");
        } else {
            clearValidationError(titleField);
        }
    }
}
```

### 3. View Model Implementation
Implement the tab's business logic:

```java
public class RequiredFieldsTabViewModel extends EntryEditorTabViewModel {
    private final ObjectProperty<String> title = new SimpleObjectProperty<>();
    private final ObjectProperty<String> author = new SimpleObjectProperty<>();
    private final ObjectProperty<String> year = new SimpleObjectProperty<>();
    private final ObjectProperty<String> entryType = new SimpleObjectProperty<>();
    private final ObservableList<String> availableEntryTypes = FXCollections.observableArrayList();

    public RequiredFieldsTabViewModel() {
        // Initialize available entry types
        availableEntryTypes.addAll(
            StandardEntryType.Article.getName(),
            StandardEntryType.Book.getName(),
            StandardEntryType.InProceedings.getName()
        );
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        // Bind to entry fields
        title.bindBidirectional(entry.getFieldBinding(StandardField.TITLE));
        author.bindBidirectional(entry.getFieldBinding(StandardField.AUTHOR));
        year.bindBidirectional(entry.getFieldBinding(StandardField.YEAR));

        // Set entry type
        entryType.set(entry.getType().getName());
    }

    @Override
    public boolean validate() {
        // Validate required fields
        return isValidTitle() && isValidAuthor() && isValidYear();
    }

    private boolean isValidTitle() {
        String titleValue = title.get();
        return titleValue != null && !titleValue.trim().isEmpty();
    }

    private boolean isValidAuthor() {
        String authorValue = author.get();
        return authorValue != null && !authorValue.trim().isEmpty();
    }

    private boolean isValidYear() {
        String yearValue = year.get();
        if (yearValue == null || yearValue.trim().isEmpty()) {
            return false;
        }
        try {
            int yearInt = Integer.parseInt(yearValue.trim());
            return yearInt >= 1000 && yearInt <= LocalDate.now().getYear() + 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
```

## Field Editor Components

### Text Field Editors
```java
public class TextFieldEditor extends VBox {
    private final TextField textField;
    private final Label label;
    private final Label errorLabel;

    public TextFieldEditor(String fieldName, StringProperty textProperty) {
        label = new Label(fieldName);
        textField = new TextField();
        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        textField.textProperty().bindBidirectional(textProperty);

        // Setup validation
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Lost focus
                validateField();
            }
        });

        getChildren().addAll(label, textField, errorLabel);
    }

    private void validateField() {
        String value = textField.getText();
        if (value == null || value.trim().isEmpty()) {
            showError("Field is required");
        } else {
            clearError();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        textField.getStyleClass().add("validation-error");
    }

    private void clearError() {
        errorLabel.setVisible(false);
        textField.getStyleClass().remove("validation-error");
    }
}
```

### Combo Box Editors
```java
public class ComboBoxEditor extends VBox {
    private final ComboBox<String> comboBox;
    private final Label label;

    public ComboBoxEditor(String fieldName, ObservableList<String> items, StringProperty selectedItem) {
        label = new Label(fieldName);
        comboBox = new ComboBox<>();
        comboBox.setItems(items);
        comboBox.valueProperty().bindBidirectional(selectedItem);

        // Make combo box editable for custom values
        comboBox.setEditable(true);

        getChildren().addAll(label, comboBox);
    }
}
```

### Multi-Line Text Editors
```java
public class TextAreaEditor extends VBox {
    private final TextArea textArea;
    private final Label label;
    private final Label charCountLabel;

    public TextAreaEditor(String fieldName, StringProperty textProperty, int maxLength) {
        label = new Label(fieldName);
        textArea = new TextArea();
        charCountLabel = new Label();

        textArea.textProperty().bindBidirectional(textProperty);
        textArea.setPrefRowCount(3);

        // Character counter
        textProperty.addListener((obs, oldVal, newVal) -> {
            int length = newVal != null ? newVal.length() : 0;
            charCountLabel.setText(length + "/" + maxLength);

            if (length > maxLength) {
                charCountLabel.getStyleClass().add("warning");
            } else {
                charCountLabel.getStyleClass().remove("warning");
            }
        });

        getChildren().addAll(label, textArea, charCountLabel);
    }
}
```

## Specialized Tab Types

### AI-Powered Tabs
```java
public class AiSummaryTab extends EntryEditorTab {
    @FXML private TextArea summaryArea;
    @FXML private Button generateButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final AiSummaryTabViewModel viewModel;

    @FXML
    public void initialize() {
        summaryArea.textProperty().bindBidirectional(viewModel.summaryProperty());
        generateButton.disableProperty().bind(viewModel.generatingProperty());
        loadingIndicator.visibleProperty().bind(viewModel.generatingProperty());
    }

    @FXML
    private void onGenerateSummary() {
        viewModel.generateSummary()
                .thenAccept(summary -> {
                    Platform.runLater(() -> {
                        // Summary is automatically bound to text area
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showError("Failed to generate summary: " + throwable.getMessage());
                    });
                    return null;
                });
    }
}
```

### File Annotation Tabs
```java
public class FileAnnotationTab extends EntryEditorTab {
    @FXML private ListView<FileAnnotation> annotationsList;
    @FXML private Button addAnnotationButton;

    @FXML
    public void initialize() {
        annotationsList.setItems(viewModel.getAnnotations());
        annotationsList.setCellFactory(list -> new FileAnnotationCell());
    }

    @FXML
    private void onAddAnnotation() {
        FileAnnotationDialog dialog = new FileAnnotationDialog();
        dialog.showAndWait().ifPresent(annotation -> {
            viewModel.addAnnotation(annotation);
        });
    }

    private static class FileAnnotationCell extends ListCell<FileAnnotation> {
        @Override
        protected void updateItem(FileAnnotation annotation, boolean empty) {
            super.updateItem(annotation, empty);

            if (empty || annotation == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(annotation.getText());
                setGraphic(createAnnotationGraphic(annotation));
            }
        }

        private Node createAnnotationGraphic(FileAnnotation annotation) {
            HBox graphic = new HBox(5);

            // Author label
            Label authorLabel = new Label(annotation.getAuthor());
            authorLabel.getStyleClass().add("annotation-author");

            // Timestamp
            Label timeLabel = new Label(formatTimestamp(annotation.getTimestamp()));
            timeLabel.getStyleClass().add("annotation-time");

            graphic.getChildren().addAll(authorLabel, timeLabel);
            return graphic;
        }
    }
}
```

## Tab Management and Navigation

### Tab Factory
```java
public class EntryEditorTabFactory {
    public static List<EntryEditorTab> createTabsForEntry(BibEntry entry) {
        List<EntryEditorTab> tabs = new ArrayList<>();

        // Always add required fields tab
        tabs.add(new RequiredFieldsTab());

        // Add optional fields tab
        tabs.add(new OptionalFieldsTab());

        // Add specialized tabs based on entry type and available features
        if (entry.getType() == StandardEntryType.Article) {
            tabs.add(new RelatedArticlesTab());
        }

        // Add AI tabs if enabled
        if (preferences.isAiEnabled()) {
            tabs.add(new AiSummaryTab());
            tabs.add(new AiKeywordsTab());
        }

        // Add file annotation tab if files are linked
        if (entry.hasField(StandardField.FILE)) {
            tabs.add(new FileAnnotationTab());
        }

        return tabs;
    }
}
```

### Tab State Management
```java
public class TabStateManager {
    private final Map<EntryEditorTab, TabState> tabStates = new HashMap<>();

    public void saveTabState(EntryEditorTab tab) {
        TabState state = new TabState();
        state.setScrollPosition(tab.getScrollPosition());
        state.setExpandedSections(tab.getExpandedSections());
        tabStates.put(tab, state);
    }

    public void restoreTabState(EntryEditorTab tab) {
        TabState state = tabStates.get(tab);
        if (state != null) {
            tab.setScrollPosition(state.getScrollPosition());
            tab.setExpandedSections(state.getExpandedSections());
        }
    }

    private static class TabState {
        private double scrollPosition;
        private Set<String> expandedSections = new HashSet<>();

        // Getters and setters
    }
}
```

## Validation and Error Handling

### Cross-Tab Validation
```java
public class EntryEditorValidator {
    public ValidationResult validateEntry(BibEntry entry, List<EntryEditorTab> tabs) {
        ValidationResult result = new ValidationResult();

        // Collect validation results from all tabs
        for (EntryEditorTab tab : tabs) {
            List<ValidationError> tabErrors = tab.validateEntry();
            result.addErrors(tabErrors);
        }

        // Perform cross-tab validation
        result.addErrors(validateCrossTabRules(entry, tabs));

        return result;
    }

    private List<ValidationError> validateCrossTabRules(BibEntry entry, List<EntryEditorTab> tabs) {
        List<ValidationError> errors = new ArrayList<>();

        // Example: If DOI is provided, URL should not be required
        boolean hasDoi = entry.hasField(StandardField.DOI);
        boolean hasUrl = entry.hasField(StandardField.URL);

        if (hasDoi && !hasUrl) {
            // DOI implies URL is not required - this is valid
        }

        return errors;
    }
}
```

### Error Display
```java
public class ValidationErrorDisplay {
    public void showValidationErrors(List<ValidationError> errors) {
        // Group errors by tab
        Map<EntryEditorTab, List<ValidationError>> errorsByTab = errors.stream()
                .collect(Collectors.groupingBy(ValidationError::getTab));

        // Show errors in each tab
        for (Map.Entry<EntryEditorTab, List<ValidationError>> entry : errorsByTab.entrySet()) {
            entry.getKey().showValidationErrors(entry.getValue());
        }

        // Highlight tabs with errors
        highlightTabsWithErrors(errorsByTab.keySet());
    }

    private void highlightTabsWithErrors(Set<EntryEditorTab> tabsWithErrors) {
        for (EntryEditorTab tab : tabsWithErrors) {
            tab.setTabStyle("tab-error");
        }
    }
}
```

## Performance Optimization

### Lazy Loading
```java
public class LazyLoadingTab extends EntryEditorTab {
    private boolean loaded = false;

    @Override
    public void setEntry(BibEntry entry) {
        super.setEntry(entry);

        // Load content only when tab becomes visible
        if (!loaded) {
            loadTabContent();
            loaded = true;
        }
    }

    private void loadTabContent() {
        // Load heavy content here
        // e.g., load file annotations, generate previews, etc.
    }
}
```

### Background Processing
```java
public class BackgroundProcessingTab extends EntryEditorTab {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void performHeavyOperation() {
        executor.submit(() -> {
            try {
                HeavyResult result = performHeavyComputation();

                Platform.runLater(() -> {
                    updateUIWithResult(result);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                });
            }
        });
    }
}
```

## Testing Entry Editor Tabs

### Unit Tests
```java
@Test
void requiredFieldsValidationWorks() {
    RequiredFieldsTabViewModel viewModel = new RequiredFieldsTabViewModel();

    // Test empty title
    viewModel.titleProperty().set("");
    assertFalse(viewModel.validate());

    // Test valid data
    viewModel.titleProperty().set("Valid Title");
    viewModel.authorProperty().set("Valid Author");
    viewModel.yearProperty().set("2023");
    assertTrue(viewModel.validate());
}
```

### UI Tests
```java
@Test
void tabShowsValidationErrors(FxRobot robot) {
    // Create tab with invalid data
    RequiredFieldsTab tab = new RequiredFieldsTab();
    BibEntry entry = new BibEntry(StandardEntryType.Article);
    tab.setEntry(entry);

    // Trigger validation
    Platform.runLater(() -> tab.validateEntry());
    WaitForAsyncUtils.waitForFxEvents();

    // Verify error display
    assertTrue(robot.lookup(".validation-error").tryQuery().isPresent());
}
```

### Integration Tests
```java
@Test
void tabSavesChangesToEntry() {
    RequiredFieldsTab tab = new RequiredFieldsTab();
    BibEntry entry = new BibEntry(StandardEntryType.Article);

    // Set values in tab
    Platform.runLater(() -> {
        tab.getTitleField().setText("New Title");
        tab.getAuthorField().setText("New Author");
    });
    WaitForAsyncUtils.waitForFxEvents();

    // Verify entry is updated
    assertEquals("New Title", entry.getTitle().orElse(""));
    assertEquals("New Author", entry.getAuthor().orElse(""));
}
```

## Accessibility Features

### Keyboard Navigation
```java
public class AccessibleTab extends EntryEditorTab {
    @Override
    public void initializeAccessibility() {
        // Set accessible labels
        titleField.setAccessibleText("Entry title");
        authorField.setAccessibleText("Entry author(s)");

        // Setup keyboard shortcuts
        getScene().setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.S) {
                // Save entry
                event.consume();
            }
        });

        // Focus management
        setFocusOrder(titleField, authorField, yearField);
    }

    private void setFocusOrder(Node... nodes) {
        for (int i = 0; i < nodes.length - 1; i++) {
            final int nextIndex = i + 1;
            nodes[i].setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
                    nodes[nextIndex].requestFocus();
                    event.consume();
                }
            });
        }
    }
}
```

### Screen Reader Support
```java
public class ScreenReaderTab extends EntryEditorTab {
    @Override
    public void setupScreenReaderSupport() {
        // Announce validation errors
        validationErrorProperty().addListener((obs, oldError, newError) -> {
            if (newError != null) {
                announceToScreenReader("Validation error: " + newError);
            }
        });

        // Describe complex UI elements
        comboBox.setAccessibleText("Entry type selector. Use arrow keys to navigate options.");
    }

    private void announceToScreenReader(String message) {
        // Use JavaFX accessibility API or third-party library
        AccessibilityUtils.announce(message);
    }
}
```

## Maintenance Guidelines

### Adding New Tabs
1. Identify the need for a new tab type
2. Create the tab controller and view model
3. Implement field editors and validation
4. Add the tab to the factory
5. Update tests and documentation

### Tab Performance
- Monitor tab loading times
- Optimize heavy computations
- Implement lazy loading for complex tabs
- Cache expensive operations

### User Experience
- Maintain consistent tab ordering
- Ensure responsive field updates
- Provide clear validation feedback
- Support keyboard and screen reader users
