package org.jabref.gui.ai.chat;

import java.util.Objects;

import javafx.beans.binding.StringExpression;
import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ListenersHelper;
import org.jabref.gui.util.MarkdownTextFlow;
import org.jabref.model.ai.chatting.ChatHistoryRecordV2;
import org.jabref.model.ai.chatting.ErrorMessage;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

public class AiChatMessageView extends HBox {
    private static final PseudoClass USER_PSEUDO_CLASS = PseudoClass.getPseudoClass("user");
    private static final PseudoClass AI_PSEUDO_CLASS = PseudoClass.getPseudoClass("ai");
    private static final PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");

    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private StackPane markdownContentPane;
    @FXML private VBox buttonsVBox;
    @FXML private Button regenerateButton;
    @FXML private Button deleteButton;

    private MarkdownTextFlow markdownTextFlow;
    private AiChatMessageViewModel viewModel;

    public AiChatMessageView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        this.viewModel = new AiChatMessageViewModel();

        markdownTextFlow = new MarkdownTextFlow(markdownContentPane);
        markdownContentPane.getChildren().add(markdownTextFlow);

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        regenerateButton.managedProperty().bind(regenerateButton.visibleProperty());
        deleteButton.managedProperty().bind(deleteButton.visibleProperty());

        sourceLabel.textProperty().bind(viewModel.sourceProperty());

        this.alignmentProperty().bind(viewModel.chatMessageProperty().map(AiChatMessageView::determineAlignment));
        buttonsVBox.visibleProperty().bind(this.hoverProperty());

        regenerateButton.visibleProperty().bind(viewModel.showRegenerateProperty());
        deleteButton.visibleProperty().bind(viewModel.showDeleteProperty());

        setupPseudoClasses();
    }

    private void setupPseudoClasses() {
        StringExpression messageType = StringExpression.stringExpression(
                chatMessageProperty().map(ChatHistoryRecordV2::messageTypeClassName)
        );

        BindingsHelper.includePseudoClassWhen(vBox, USER_PSEUDO_CLASS, messageType.isEqualTo(UserMessage.class.getName()));
        BindingsHelper.includePseudoClassWhen(vBox, AI_PSEUDO_CLASS, messageType.isEqualTo(AiMessage.class.getName()));
        BindingsHelper.includePseudoClassWhen(vBox, ERROR_PSEUDO_CLASS, messageType.isEqualTo(ErrorMessage.class.getName()));
    }

    private void setupListeners() {
        ListenersHelper.onChangeNonNull(viewModel.chatMessageProperty(), this::updateOrder);
        ListenersHelper.onChangeNonNull(viewModel.chatMessageProperty(), this::updateContent);
    }

    private void updateOrder(ChatHistoryRecordV2 chatMessage) {
        String type = chatMessage.messageTypeClassName();
        boolean isUser = Objects.equals(type, UserMessage.class.getName());

        this.getChildren().clear();
        if (isUser) {
            this.getChildren().addAll(buttonsVBox, vBox);
        } else {
            this.getChildren().addAll(vBox, buttonsVBox);
        }
    }

    private void updateContent(ChatHistoryRecordV2 chatMessage) {
        markdownTextFlow.setMarkdown(chatMessage.content());
    }

    private static Pos determineAlignment(ChatHistoryRecordV2 chatMessage) {
        if (Objects.equals(chatMessage.messageTypeClassName(), UserMessage.class.getName())) {
            return Pos.TOP_RIGHT;
        } else {
            return Pos.TOP_LEFT;
        }
    }

    @FXML
    private void onDeleteClick() {
        viewModel.delete();
    }

    @FXML
    private void onRegenerateClick() {
        viewModel.regenerate();
    }

    public ObjectProperty<ChatHistoryRecordV2> chatMessageProperty() {
        return viewModel.chatMessageProperty();
    }

    public ChatHistoryRecordV2 getChatMessage() {
        return viewModel.chatMessageProperty().get();
    }

    public void setChatMessage(ChatHistoryRecordV2 chatMessage) {
        viewModel.chatMessageProperty().set(chatMessage);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onDeleteProperty() {
        return viewModel.onDeleteProperty();
    }

    public EventHandler<ActionEvent> getOnDelete() {
        return viewModel.onDeleteProperty().get();
    }

    public void setOnDelete(EventHandler<ActionEvent> onDelete) {
        viewModel.onDeleteProperty().set(onDelete);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return viewModel.onRegenerateProperty();
    }

    public EventHandler<ActionEvent> getOnRegenerate() {
        return viewModel.onRegenerateProperty().get();
    }

    public void setOnRegenerate(EventHandler<ActionEvent> onRegenerate) {
        viewModel.onRegenerateProperty().set(onRegenerate);
    }
}
