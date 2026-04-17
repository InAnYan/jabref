package org.jabref.gui.ai.chatmodelconfig;

import java.util.List;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.textfield.CustomPasswordField;

/// Self-loading reusable component that exposes the full chat-model configuration UI
/// including profile management (add / rename / delete profiles).
///
/// Usage (embed in another FXML):
/// <pre>{@code <ChatModelConfigView fx:id="chatModelConfigView"/>}</pre>
///
/// Call {@link #loadFrom(AiPreferences)} after FXML initialisation to seed from global prefs.
public class ChatModelConfigView extends VBox {

    private static final String HUGGING_FACE_PROMPT = "TinyLlama/TinyLlama_v1.1 (or any other model name)";

    @FXML private ComboBox<String> profileComboBox;
    @FXML private Button renameProfileButton;
    @FXML private Button deleteProfileButton;

    @FXML private ComboBox<AiProvider> providerComboBox;
    @FXML private ComboBox<String> chatModelComboBox;
    @FXML private CustomPasswordField apiKeyTextField;
    @FXML private TextField apiBaseUrlTextField;
    @FXML private TextField temperatureTextField;
    @FXML private IntegerInputField contextWindowSizeTextField;
    @FXML private ComboBox<TokenEstimatorKind> tokenEstimatorComboBox;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    private ChatModelConfigViewModel viewModel;

    public ChatModelConfigView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new ChatModelConfigViewModel();

        setupBindings();
        setupValidations();
    }

    private void setupBindings() {
        profileComboBox.setItems(viewModel.profileNamesProperty());
        profileComboBox.setEditable(false);

        // Bind selected index bidirectionally via listener (IntegerProperty ↔ SingleSelectionModel)
        viewModel.selectedProfileIndexProperty().addListener((_, _, newVal) -> {
            int idx = newVal.intValue();
            if (profileComboBox.getSelectionModel().getSelectedIndex() != idx) {
                profileComboBox.getSelectionModel().select(idx);
            }
        });

        profileComboBox.getSelectionModel().selectedIndexProperty().addListener((_, _, newVal) -> {
            int idx = newVal.intValue();
            if (viewModel.selectedProfileIndexProperty().get() != idx && idx >= 0) {
                viewModel.selectedProfileIndexProperty().set(idx);
            }
        });

        deleteProfileButton.disableProperty().bind(
                Bindings.size(viewModel.profileNamesProperty()).lessThanOrEqualTo(1));

        new ViewModelListCellFactory<AiProvider>()
                .withText(AiProvider::toString)
                .install(providerComboBox);
        providerComboBox.itemsProperty().bind(viewModel.providersProperty());
        providerComboBox.valueProperty().bindBidirectional(viewModel.selectedProviderProperty());

        new ViewModelListCellFactory<String>()
                .withText(text -> text)
                .install(chatModelComboBox);
        chatModelComboBox.itemsProperty().bind(viewModel.chatModelsProperty());
        chatModelComboBox.valueProperty().bindBidirectional(viewModel.selectedChatModelProperty());

        chatModelComboBox.promptTextProperty().bind(
                Bindings.when(viewModel.selectedProviderProperty()
                                       .isEqualTo(AiProvider.HUGGING_FACE))
                        .then(HUGGING_FACE_PROMPT)
                        .otherwise(""));

        apiKeyTextField.textProperty().bindBidirectional(viewModel.apiKeyProperty());

        apiBaseUrlTextField.textProperty().bindBidirectional(viewModel.apiBaseUrlProperty());
        apiBaseUrlTextField.disableProperty().bind(viewModel.disableApiBaseUrlProperty());

        temperatureTextField.textProperty().bindBidirectional(viewModel.temperatureProperty());

        contextWindowSizeTextField.valueProperty().addListener((_, _, newVal) ->
                viewModel.contextWindowSizeProperty().set(newVal == null ? 0 : newVal));
        viewModel.contextWindowSizeProperty().addListener((_, _, newVal) ->
                contextWindowSizeTextField.valueProperty().set(newVal == null ? 0 : newVal.intValue()));

        new ViewModelListCellFactory<TokenEstimatorKind>()
                .withText(TokenEstimatorKind::name)
                .install(tokenEstimatorComboBox);
        tokenEstimatorComboBox.itemsProperty().bind(viewModel.tokenEstimatorKindsProperty());
        tokenEstimatorComboBox.valueProperty().bindBidirectional(viewModel.selectedTokenEstimatorKindProperty());
    }

    private void setupValidations() {
        // Validators are returned by ChatModelConfigViewModel.getValidators() in this fixed order:
        //   apiKey, chatModel, apiBaseUrl, temperatureType, temperatureRange, contextWindowSize
        // The controls list must match that order exactly.
        List<Control> controls = List.of(
                apiKeyTextField,
                chatModelComboBox,
                apiBaseUrlTextField,
                temperatureTextField,
                temperatureTextField,        // temperatureRange shares the same field
                contextWindowSizeTextField
        );

        Platform.runLater(() -> {
            List.of(0, 1, 2, 3, 4, 5).forEach(i ->
                    visualizer.initVisualization(
                            viewModel.getValidators().get(i).getValidationStatus(),
                            controls.get(i)
                    )
            );
        });
    }

    @FXML
    private void onAddProfile() {
        viewModel.addProfile();
    }

    @FXML
    private void onDeleteProfile() {
        viewModel.removeProfile(viewModel.selectedProfileIndexProperty().get());
    }

    @FXML
    private void onRenameProfile() {
        if (profileComboBox.isEditable()) {
            String newName = profileComboBox.getEditor().getText();
            viewModel.renameCurrentProfile(newName);
            profileComboBox.setEditable(false);
            renameProfileButton.setText("Rename");
        } else {
            profileComboBox.setEditable(true);
            profileComboBox.getEditor().selectAll();
            profileComboBox.getEditor().requestFocus();
            renameProfileButton.setText("OK");

            profileComboBox.getEditor().setOnAction(_ -> onRenameProfile());
            profileComboBox.getEditor().focusedProperty().addListener((_, _, focused) -> {
                if (!focused && profileComboBox.isEditable()) {
                    onRenameProfile();
                }
            });
        }
    }

    public void loadFrom(AiPreferences prefs) {
        viewModel.loadFrom(prefs);
    }

    public void storeInto(AiPreferences prefs) {
        viewModel.storeInto(prefs);
    }

    public void resetToDefaults() {
        viewModel.resetToDefaults();
    }

    public ChatModelConfigViewModel getViewModel() {
        return viewModel;
    }

    public ObservableValue<ChatModel> chatModelProperty() {
        return viewModel.chatModelProperty();
    }
}










