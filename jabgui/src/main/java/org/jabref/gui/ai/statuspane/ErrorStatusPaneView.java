package org.jabref.gui.ai.statuspane;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * Shows title, description, text area, and restart and cancel buttons.
 */
public class ErrorStatusPaneView extends BorderPane {
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private TextArea textArea;
    @FXML private Button restartButton;
    @FXML private Button cancelButton;

    private ErrorStatusPaneViewModel viewModel;

    public ErrorStatusPaneView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new ErrorStatusPaneViewModel();

        setupBindings();
    }

    private void setupBindings() {
        titleLabel.managedProperty().bind(titleLabel.visibleProperty());
        descriptionLabel.managedProperty().bind(descriptionLabel.visibleProperty());
        textArea.managedProperty().bind(textArea.visibleProperty());
        restartButton.managedProperty().bind(restartButton.visibleProperty());
        cancelButton.managedProperty().bind(cancelButton.visibleProperty());

        titleLabel.visibleProperty().bind(viewModel.titleProperty().isNotEmpty());
        descriptionLabel.visibleProperty().bind(viewModel.descriptionProperty().isNotEmpty());
        textArea.visibleProperty().bind(viewModel.exceptionStringProperty().isNotEmpty());
        restartButton.visibleProperty().bind(viewModel.restartButtonTextProperty().isNotEmpty());
        cancelButton.visibleProperty().bind(viewModel.cancelButtonTextProperty().isNotEmpty());

        titleLabel.textProperty().bind(viewModel.titleProperty());
        descriptionLabel.textProperty().bind(viewModel.descriptionProperty());
        textArea.textProperty().bind(viewModel.exceptionStringProperty());
        restartButton.textProperty().bind(viewModel.restartButtonTextProperty());
        cancelButton.textProperty().bind(viewModel.cancelButtonTextProperty());
    }

    public StringProperty titleProperty() {
        return viewModel.titleProperty();
    }

    public String getTitle() {
        return viewModel.titleProperty().get();
    }

    public void setTitle(String title) {
        viewModel.titleProperty().set(title);
    }

    public StringProperty descriptionProperty() {
        return viewModel.descriptionProperty();
    }

    public String getDescription() {
        return viewModel.descriptionProperty().get();
    }

    public void setDescription(String description) {
        viewModel.descriptionProperty().set(description);
    }

    public ObjectProperty<Exception> exceptionProperty() {
        return viewModel.exceptionProperty();
    }

    public StringProperty restartButtonTextProperty() {
        return viewModel.restartButtonTextProperty();
    }

    public String getRestartButtonText() {
        return viewModel.restartButtonTextProperty().get();
    }

    public void setRestartButtonText(String restartButtonText) {
        viewModel.restartButtonTextProperty().set(restartButtonText);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRestartProperty() {
        return viewModel.onRestartProperty();
    }

    public EventHandler<ActionEvent> getOnRestart() {
        return viewModel.onRestartProperty().get();
    }

    public void setOnRestart(EventHandler<ActionEvent> onRestart) {
        viewModel.onRestartProperty().set(onRestart);
    }

    public StringProperty cancelButtonTextProperty() {
        return viewModel.cancelButtonTextProperty();
    }

    public String getCancelButtonText() {
        return viewModel.cancelButtonTextProperty().get();
    }

    public void setCancelButtonText(String cancelButtonText) {
        viewModel.cancelButtonTextProperty().set(cancelButtonText);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onCancelProperty() {
        return viewModel.onCancelProperty();
    }

    public EventHandler<ActionEvent> getOnCancel() {
        return viewModel.onCancelProperty().get();
    }

    public void setOnCancel(EventHandler<ActionEvent> onCancel) {
        viewModel.onCancelProperty().set(onCancel);
    }

    @FXML
    private void restart() {
        viewModel.restart();
    }

    @FXML
    private void cancel() {
        viewModel.cancel();
    }
}
