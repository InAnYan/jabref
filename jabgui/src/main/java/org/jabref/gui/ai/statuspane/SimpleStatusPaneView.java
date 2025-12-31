package org.jabref.gui.ai.statuspane;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * Shows title and description.
 */
public class SimpleStatusPaneView extends BorderPane {
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;

    private SimpleStatusPaneViewModel viewModel;

    public SimpleStatusPaneView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new SimpleStatusPaneViewModel();

        setupBindings();
    }

    private void setupBindings() {
        titleLabel.textProperty().bind(viewModel.titleProperty());
        descriptionLabel.textProperty().bind(viewModel.descriptionProperty());
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
}
