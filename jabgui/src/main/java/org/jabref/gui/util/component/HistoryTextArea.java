package org.jabref.gui.util.component;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/// A custom JavaFX TextArea that provides shell-like history navigation, smart multiline handling,
/// and auto-expanding height.
///
/// **Behavioral Properties:**
///
/// - **Auto-Sizing:** Starts with a height of 1 row. Automatically expands vertically as new lines are added (up to a maximum of 10 rows), then shows a scrollbar.
/// - **Submission:** Pressing `ENTER` submits the text (triggers the callback) and clears the input.
/// - **New Lines:** Pressing `SHIFT+ENTER` inserts a new line without submitting.
/// - **History Navigation (UP/DOWN):**
///
/// - **UP Arrow:** Recalls the previous message from history. Places the cursor at the **END** of the text.
/// - **DOWN Arrow:** Recalls the next (newer) message. Places the cursor at the **START** of the text.
///
///
/// - **Smart Multiline Navigation:**
///
/// - If the text contains multiple lines, `UP` and `DOWN` will move the cursor purely visually between lines first.
/// - History navigation is only triggered when the cursor is at the physical top boundary (for UP) or bottom boundary (for DOWN).
///
///
/// - **"Browsing" vs. "Edit" Mode:**
///
/// - **Browsing Mode:** Active when scrolling through history. You can move up and down freely.
/// - **Edit Mode:** Triggered immediately if you type a character, delete text, or paste content while viewing a history entry.
/// - **Locking Behavior:** Once in Edit Mode, the component "forgets" the link to the history list. Pressing UP/DOWN will **not** jump to a different message; it will simply move the cursor within the current draft. This prevents accidental data loss.
///
public class HistoryTextArea extends TextArea {

    private static final int NEW_MESSAGE_INDEX = -1;
    private static final int MAX_EXPAND_ROWS = 10;

    private final ListProperty<String> history = new SimpleListProperty<>(FXCollections.observableArrayList());
    private int currentHistoryIndex = NEW_MESSAGE_INDEX;
    private boolean isBrowsingHistory = false;
    private final ObjectProperty<EventHandler<ActionEvent>> onSubmit = new SimpleObjectProperty<>();

    public HistoryTextArea() {
        super();
        this.setWrapText(true);
        this.setPrefRowCount(1);

        this.textProperty().addListener((observable, oldValue, newValue) -> {
            int newLines = 1;
            if (newValue != null) {
                for (int i = 0; i < newValue.length(); i++) {
                    if (newValue.charAt(i) == '\n') {
                        newLines++;
                    }
                }
            }
            this.setPrefRowCount(Math.min(newLines, MAX_EXPAND_ROWS));
        });

        history.addListener((obs, oldVal, newVal) -> resetHistoryState());
        this.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public void setOnSubmit(EventHandler<ActionEvent> onSubmit) {
        this.onSubmit.set(onSubmit);
    }

    public EventHandler<ActionEvent> getOnSubmit() {
        return onSubmit.get();
    }

    public ObservableList<String> getHistory() {
        return history.get();
    }

    private void handleKeyPressed(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();

        if (code == KeyCode.DOWN) {
            handleDownKey(keyEvent);
        } else if (code == KeyCode.UP) {
            handleUpKey(keyEvent);
        } else if (code == KeyCode.ENTER) {
            handleEnterKey(keyEvent);
        } else {
            boolean isNavigation = code.isNavigationKey();
            boolean isModifier = code.isModifierKey();

            if (!isNavigation && !isModifier) {
                isBrowsingHistory = false;
                currentHistoryIndex = NEW_MESSAGE_INDEX;
            }
        }
    }

    private void handleEnterKey(KeyEvent event) {
        if (event.isShiftDown()) {
            this.replaceSelection("\n");
            event.consume();
            return;
        }

        event.consume();
        String text = this.getText().trim();

        if (!text.isEmpty() && onSubmit.get() != null) {
            resetHistoryState();
            onSubmit.get().handle(new ActionEvent());
        }
    }

    private void handleUpKey(KeyEvent event) {
        int caret = getCaretPosition();
        String textBeforeCaret = getText().substring(0, caret);
        if (textBeforeCaret.contains("\n")) {
            return;
        }

        boolean canGoBack = (currentHistoryIndex < history.getSize() - 1);
        boolean shouldJump = getText().isEmpty() || isBrowsingHistory;

        if (canGoBack && shouldJump) {
            isBrowsingHistory = true;
            currentHistoryIndex++;
            updateTextFromHistory(false);
            event.consume();
        }
    }

    private void handleDownKey(KeyEvent event) {
        int caret = getCaretPosition();
        String textAfterCaret = getText().substring(caret);
        if (textAfterCaret.contains("\n")) {
            return;
        }

        if (currentHistoryIndex != NEW_MESSAGE_INDEX) {
            isBrowsingHistory = true;
            currentHistoryIndex--;

            if (currentHistoryIndex == NEW_MESSAGE_INDEX) {
                this.clear();
            } else {
                updateTextFromHistory(true);
            }
            event.consume();
        }
    }

    private void updateTextFromHistory(boolean setCursorAtStart) {
        if (currentHistoryIndex >= 0 && currentHistoryIndex < history.getSize()) {
            String msg = history.get(currentHistoryIndex);
            this.setText(msg);

            if (setCursorAtStart) {
                this.positionCaret(0);
            } else {
                this.positionCaret(this.getText().length());
            }
        }
    }

    private void resetHistoryState() {
        currentHistoryIndex = NEW_MESSAGE_INDEX;
        isBrowsingHistory = false;
    }
}
