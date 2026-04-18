package org.jabref.gui.ai.chat;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.LocaleUtil;
import org.jabref.gui.util.component.MarkdownTextFlow;
import org.jabref.model.ai.chatting.ChatMessage;

import com.airhacks.afterburner.views.ViewLoader;

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

    // Tooltip for the whole component.
    private Tooltip tooltip = new Tooltip();
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

        Tooltip.install(vBox, tooltip);

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        regenerateButton.managedProperty().bind(regenerateButton.visibleProperty());
        deleteButton.managedProperty().bind(deleteButton.visibleProperty());

        sourceLabel.textProperty().bind(viewModel.sourceProperty());
        tooltip.textProperty().bind(viewModel.timestampProperty().map(LocaleUtil::formatInstant));

        this.alignmentProperty().bind(viewModel.chatMessageProperty().map(AiChatMessageView::determineAlignment));
        buttonsVBox.visibleProperty().bind(this.hoverProperty());

        regenerateButton.visibleProperty().bind(viewModel.showRegenerateProperty());
        deleteButton.visibleProperty().bind(viewModel.showDeleteProperty());

        setupPseudoClasses();
    }

    private void setupPseudoClasses() {
        ObservableValue<ChatMessage.Role> messageRole = chatMessageProperty().map(ChatMessage::role);

        ObservableValue<Boolean> isUser = messageRole.map(v -> v == ChatMessage.Role.USER);
        ObservableValue<Boolean> isAi = messageRole.map(v -> v == ChatMessage.Role.AI);
        ObservableValue<Boolean> isError = messageRole.map(v -> v == ChatMessage.Role.ERROR);

        BindingsHelper.includePseudoClassWhen(vBox, USER_PSEUDO_CLASS, isUser.orElse(false));
        BindingsHelper.includePseudoClassWhen(vBox, AI_PSEUDO_CLASS, isAi.orElse(false));
        BindingsHelper.includePseudoClassWhen(vBox, ERROR_PSEUDO_CLASS, isError.orElse(false));
    }

    private void setupListeners() {
        BindingsHelper.onChangeNonNull(viewModel.chatMessageProperty(), this::updateOrder);
        BindingsHelper.onChangeNonNull(viewModel.chatMessageProperty(), this::updateContent);
    }

    private void updateOrder(ChatMessage chatMessage) {
        this.getChildren().clear();

        if (chatMessage.role() == ChatMessage.Role.USER) {
            this.getChildren().addAll(buttonsVBox, vBox);
        } else {
            this.getChildren().addAll(vBox, buttonsVBox);
        }
    }

    private void updateContent(ChatMessage chatMessage) {
        markdownTextFlow.setMarkdown(chatMessage.content());
    }

    private static Pos determineAlignment(ChatMessage chatMessage) {
        if (chatMessage.role() == ChatMessage.Role.USER) {
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

    public ObjectProperty<ChatMessage> chatMessageProperty() {
        return viewModel.chatMessageProperty();
    }

    public ChatMessage getChatMessage() {
        return viewModel.chatMessageProperty().get();
    }

    public void setChatMessage(ChatMessage chatMessage) {
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
