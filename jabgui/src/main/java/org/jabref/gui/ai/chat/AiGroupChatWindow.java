package org.jabref.gui.ai.chat;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;

import org.jabref.gui.groups.GroupNodeViewModel;
import org.jabref.gui.util.BaseDialog;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;

public class AiGroupChatWindow extends BaseDialog<Void> {
    @FXML private AiGroupChatView chatView;

    public AiGroupChatWindow() {
        super();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.titleProperty().bind(chatView.windowTitleProperty());
    }

    public ObjectProperty<GroupNodeViewModel> groupNodeProperty() {
        return chatView.groupNodeProperty();
    }

    public ObjectProperty<BibDatabaseContext> databaseContextProperty() {
        return chatView.databaseContextProperty();
    }
}
