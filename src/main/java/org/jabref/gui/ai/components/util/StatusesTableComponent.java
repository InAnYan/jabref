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
        pathColumn.setMaxWidth(300);

        TableColumn<ProcessingStatus, String> stateColumn = new TableColumn<>(Localization.lang("State"));
        stateColumn.setCellValueFactory(
                new PropertyValueFactory<>("processingState")
        );
        stateColumn.setMaxWidth(100);

        TableColumn<ProcessingStatus, String> messageColumn = new TableColumn<>(Localization.lang("Message"));
        messageColumn.setCellValueFactory(
                new PropertyValueFactory<>("message")
        );
        messageColumn.setMaxWidth(300);

        tableView.setEditable(false);
        tableView.setItems(statuses);
        tableView.getColumns().addAll(pathColumn, stateColumn, messageColumn);
        tableView.setMaxWidth(700);
        tableView.setMinWidth(700);
        tableView.setMaxHeight(200);
    }

    public Node getTable() {
        return tableView;
    }
}
