package org.jabref.gui.ai.statuspane;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * Shows title, description, and progress indicator.
 */
public class LoadingStatusPaneView extends BorderPane {
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;

    private LoadingStatusPaneViewModel viewModel;

    public LoadingStatusPaneView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new LoadingStatusPaneViewModel();

        setupBindings();
    }

    private void setupBindings() {
        titleLabel.textProperty().bind(viewModel.titleProperty());
        descriptionLabel.textProperty().bind(viewModel.descriptionProperty());
    }

    @FXML
    private void cancel() {
        viewModel.cancel();
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

    public ObjectProperty<EventHandler<ActionEvent>> onCancelProperty() {
        return viewModel.onCancelProperty();
    }

    public EventHandler<ActionEvent> getOnCancel() {
        return viewModel.onCancelProperty().get();
    }

    public void setOnCancel(EventHandler<ActionEvent> onCancel) {
        viewModel.onCancelProperty().set(onCancel);
    }
}
