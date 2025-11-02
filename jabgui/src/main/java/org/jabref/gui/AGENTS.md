# Implementing GUI Components

This guide outlines the process for implementing graphical user interface components in JabRef, including MVVM patterns, JavaFX usage, accessibility, and testing.

## GUI Architecture Overview

JabRef uses a Model-View-ViewModel (MVVM) architecture with JavaFX:

### Core Components

- **View**: FXML files defining UI structure
- **ViewModel**: Business logic and state management
- **Model**: Data entities and business logic
- **Controller**: Thin layer coordinating View and ViewModel

### Package Structure

```
gui/
├── actions/          # User actions and commands
├── dialogs/          # Modal dialogs
├── entryeditor/      # Entry editing components
├── fields/           # Field-specific editors
├── preferences/      # Settings dialogs
└── util/            # GUI utilities
```

## Implementation Process

### 1. Component Planning

- Define the user interaction and workflow
- Identify required UI elements and layout
- Plan state management and data binding
- Consider accessibility requirements

### 2. ViewModel Implementation

Create the ViewModel following established patterns:

```java
public class MyFeatureViewModel extends AbstractViewModel {
    private final ObjectProperty<String> searchText = new SimpleObjectProperty<>();
    private final BooleanProperty isSearching = new SimpleBooleanProperty(false);
    private final ObservableList<MyItem> items = FXCollections.observableArrayList();

    public MyFeatureViewModel() {
        // Initialize bindings and listeners
        searchText.addListener((obs, oldValue, newValue) -> performSearch(newValue));
    }

    public void performSearch(String query) {
        isSearching.set(true);
        // Async search implementation
        searchService.search(query)
                .thenAccept(results -> {
                    Platform.runLater(() -> {
                        items.setAll(results);
                        isSearching.set(false);
                    });
                });
    }

    // Property accessors for data binding
    public ObjectProperty<String> searchTextProperty() { return searchText; }
    public BooleanProperty isSearchingProperty() { return isSearching; }
    public ObservableList<MyItem> getItems() { return items; }
}
```

### 3. View Implementation

Create FXML view with proper structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox xmlns="http://javafx.com/javafx/11.0.1" xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="org.jabref.gui.myfeature.MyFeatureController"
      spacing="10" prefWidth="600" prefHeight="400">

    <TextField fx:id="searchField" promptText="Search..."
               text="${controller.viewModel.searchText}"/>

    <TableView fx:id="resultsTable" VBox.vgrow="ALWAYS"
               items="${controller.viewModel.items}">
        <columns>
            <TableColumn text="Title" prefWidth="200">
                <cellValueFactory>
                    <PropertyValueFactory property="title"/>
                </cellValueFactory>
            </TableColumn>
        </columns>
    </TableView>

    <HBox spacing="10" alignment="CENTER_RIGHT">
        <Button text="Cancel" onAction="#onCancel"/>
        <Button text="OK" styleClass="primary" onAction="#onOK"/>
    </HBox>
</VBox>
```

### 4. Controller Implementation

Create thin controller for coordination:

```java
public class MyFeatureController {
    @FXML private TextField searchField;
    @FXML private TableView<MyItem> resultsTable;

    private MyFeatureViewModel viewModel;

    @FXML
    public void initialize() {
        viewModel = new MyFeatureViewModel();
        searchField.textProperty().bindBidirectional(viewModel.searchTextProperty());
        resultsTable.setItems(viewModel.getItems());
    }

    @FXML
    private void onOK() {
        MyItem selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Handle selection
            closeDialog();
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        // Close dialog logic
    }
}
```

## Data Binding Patterns

### Property Binding

```java
// ViewModel properties
private final StringProperty title = new SimpleStringProperty();
private final BooleanProperty enabled = new SimpleBooleanProperty(true);
private final ObservableList<String> options = FXCollections.observableArrayList();

// Controller binding
@FXML
public void initialize() {
    titleField.textProperty().bindBidirectional(viewModel.titleProperty());
    okButton.disableProperty().bind(viewModel.enabledProperty().not());
    optionComboBox.setItems(viewModel.getOptions());
}
```

### List Binding

```java
// Observable list in ViewModel
private final ObservableList<BibEntry> entries = FXCollections.observableArrayList();

// Table binding with custom cell factories
@FXML
public void initialize() {
    titleColumn.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
    authorColumn.setCellValueFactory(cellData -> cellData.getValue().authorProperty());

    // Custom cell factory for formatting
    titleColumn.setCellFactory(column -> new TableCell<>() {
        @Override
        protected void updateItem(String title, boolean empty) {
            super.updateItem(title, empty);
            setText(empty ? null : formatTitle(title));
        }
    });
}
```

## Dialog Implementation

### Modal Dialogs

```java
public class MyDialog extends FXDialog {
    private final MyDialogViewModel viewModel;

    public MyDialog() {
        this.viewModel = new MyDialogViewModel();
        init();
    }

    private void init() {
        VBox content = new VBox(10);
        content.getChildren().addAll(
            createInputField(),
            createButtonBar()
        );

        DialogPane dialogPane = new DialogPane();
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setDialogPane(dialogPane);
        setResultConverter(this::convertResult);
    }

    private ButtonBar createButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        // Configure buttons
        return buttonBar;
    }
}
```

### Dialog Services

Use JabRef's dialog service for consistency:

```java
public class MyFeatureAction implements Action {
    private final DialogService dialogService;

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new MyDialog())
                .ifPresent(result -> handleResult(result));
    }
}
```

## Form Validation

### Real-time Validation

```java
public class ValidationViewModel extends AbstractViewModel {
    private final StringProperty inputText = new SimpleStringProperty();
    private final StringProperty errorMessage = new SimpleStringProperty();

    public ValidationViewModel() {
        inputText.addListener((obs, oldValue, newValue) -> validateInput(newValue));
    }

    private void validateInput(String value) {
        if (value == null || value.trim().isEmpty()) {
            errorMessage.set("Field is required");
        } else if (value.length() < 3) {
            errorMessage.set("Must be at least 3 characters");
        } else {
            errorMessage.set(null); // Clear error
        }
    }

    public boolean isValid() {
        return errorMessage.get() == null;
    }
}
```

### Visual Feedback

```java
@FXML
public void initialize() {
    // Bind validation styling
    inputField.styleClassProperty().bind(
        Bindings.when(viewModel.errorMessageProperty().isNotNull())
                .then("validation-error")
                .otherwise("")
    );

    // Show error message
    errorLabel.textProperty().bind(viewModel.errorMessageProperty());
    errorLabel.visibleProperty().bind(viewModel.errorMessageProperty().isNotNull());
}
```

## Asynchronous Operations

### Background Tasks

```java
public class AsyncViewModel extends AbstractViewModel {
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    public void performAsyncOperation() {
        loading.set(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Long-running operation
                Thread.sleep(2000);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    loading.set(false);
                    // Update UI with results
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    loading.set(false);
                    // Handle error
                });
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
```

### Progress Indication

```java
// Progress bar binding
progressBar.progressProperty().bind(viewModel.progressProperty());
progressBar.visibleProperty().bind(viewModel.loadingProperty());

// Indeterminate progress for unknown duration
progressIndicator.visibleProperty().bind(viewModel.loadingProperty());
```

## Accessibility

### Keyboard Navigation

```java
@FXML
public void initialize() {
    // Set focus order
    searchField.setFocusTraversable(true);
    resultsTable.setFocusTraversable(true);

    // Keyboard shortcuts
    getScene().setOnKeyPressed(event -> {
        if (event.isControlDown() && event.getCode() == KeyCode.F) {
            searchField.requestFocus();
            event.consume();
        }
    });
}
```

### Screen Reader Support

```java
// Accessible labels
searchField.setAccessibleText("Search for items");
resultsTable.setAccessibleText("Search results table");

// Role assignment
resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
    if (newVal != null) {
        // Announce selection to screen readers
    }
});
```

## Styling and Theming

### CSS Classes

```java
// Component styling
@FXML
public void initialize() {
    root.getStyleClass().add("my-feature-dialog");
    searchField.getStyleClass().add("search-input");
    resultsTable.getStyleClass().add("data-table");
}
```

### Theme Integration

```java
// Theme-aware styling
private void updateTheme(boolean darkMode) {
    root.getStyleClass().removeAll("light-theme", "dark-theme");
    root.getStyleClass().add(darkMode ? "dark-theme" : "light-theme");
}
```

## Testing GUI Components

### Unit Testing ViewModels

```java
@Test
void searchUpdatesResults() {
    MyViewModel viewModel = new MyViewModel();

    viewModel.searchTextProperty().set("test query");

    // Wait for async operation
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(viewModel.getItems().isEmpty());
}
```

### UI Testing

```java
@Test
void dialogShowsAndCloses(FxRobot robot) {
    // Show dialog
    MyDialog dialog = new MyDialog();
    Platform.runLater(dialog::show);

    // Verify dialog is visible
    assertTrue(dialog.isShowing());

    // Simulate user interaction
    robot.clickOn(".ok-button");

    // Verify dialog closed
    assertFalse(dialog.isShowing());
}
```

## Common Patterns

### Table Components

```java
private TableView<BibEntry> createEntryTable() {
    TableView<BibEntry> table = new TableView<>();

    TableColumn<BibEntry, String> titleCol = new TableColumn<>("Title");
    titleCol.setCellValueFactory(data -> data.getValue().titleProperty());
    titleCol.setPrefWidth(200);

    TableColumn<BibEntry, String> authorCol = new TableColumn<>("Author");
    authorCol.setCellValueFactory(data -> data.getValue().authorProperty());
    authorCol.setPrefWidth(150);

    table.getColumns().addAll(titleCol, authorCol);
    return table;
}
```

### Form Components

```java
private VBox createForm() {
    VBox form = new VBox(10);

    TextField titleField = new TextField();
    titleField.setPromptText("Enter title");

    TextArea descriptionArea = new TextArea();
    descriptionArea.setPromptText("Enter description");
    descriptionArea.setPrefRowCount(3);

    ComboBox<String> typeCombo = new ComboBox<>();
    typeCombo.getItems().addAll("Type A", "Type B", "Type C");

    form.getChildren().addAll(
        new Label("Title:"), titleField,
        new Label("Description:"), descriptionArea,
        new Label("Type:"), typeCombo
    );

    return form;
}
```

### Action Buttons

```java
private HBox createActionButtons() {
    HBox buttonBox = new HBox(10);
    buttonBox.setAlignment(Pos.CENTER_RIGHT);

    Button cancelBtn = new Button("Cancel");
    cancelBtn.setOnAction(e -> onCancel());

    Button okBtn = new Button("OK");
    okBtn.setDefaultButton(true);
    okBtn.setOnAction(e -> onOK());

    buttonBox.getChildren().addAll(cancelBtn, okBtn);
    return buttonBox;
}
```

## Performance Considerations

### Lazy Loading

```java
private void setupLazyLoading(TableView<?> table) {
    table.setItems(FXCollections.observableArrayList());

    // Load data in chunks
    table.scrollTo(0); // Trigger initial load

    table.getSelectionModel().selectedIndexProperty().addListener((obs, old, newIndex) -> {
        if (newIndex.intValue() > table.getItems().size() - 10) {
            loadMoreData();
        }
    });
}
```

### Memory Management

```java
// Clean up listeners to prevent memory leaks
@Override
public void dispose() {
    searchText.removeListener(searchListener);
    items.removeListener(itemsListener);
    // Clean up other resources
}
```
