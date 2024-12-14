package org.jabref.gui.ai.components.util;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import org.jabref.logic.l10n.Localization;

public class StatusesTableComponent {
    private final TableView<ProcessingStatus> tableView = new TableView<>();

    public StatusesTableComponent(ObservableList<ProcessingStatus> statuses) {
        TableColumn<ProcessingStatus, String> pathColumn = new TableColumn<>(Localization.lang("File"));
        pathColumn.setCellValueFactory(
                new PropertyValueFactory<>("path")
        );

        TableColumn<ProcessingStatus, String> statusColumn = new TableColumn<>(Localization.lang("Status"));
        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        TableColumn<ProcessingStatus, String> messageColumn = new TableColumn<>(Localization.lang("Message"));
        messageColumn.setCellValueFactory(
                new PropertyValueFactory<>("message")
        );

        tableView.setEditable(false);
        tableView.setItems(statuses);
        tableView.getColumns().addAll(pathColumn, statusColumn, messageColumn);
    }

    public Node getTable() {
        return tableView;
    }
}
