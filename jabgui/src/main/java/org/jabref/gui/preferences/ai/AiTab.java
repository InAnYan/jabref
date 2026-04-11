package org.jabref.gui.preferences.ai;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.embeddings.EmbeddingModelEnumeration;
import org.jabref.model.ai.llm.AiProvider;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.textfield.CustomPasswordField;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    private static final String HUGGING_FACE_CHAT_MODEL_PROMPT = "TinyLlama/TinyLlama_v1.1 (or any other model name)";
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    @FXML private CheckBox enableAi;
    @FXML private CheckBox autoGenerateEmbeddings;
    @FXML private CheckBox autoGenerateSummaries;
    @FXML private ComboBox<AiProvider> aiProviderComboBox;
    @FXML private ComboBox<String> chatModelComboBox;
    @FXML private CustomPasswordField apiKeyTextField;
    @FXML private CheckBox customizeExpertSettingsCheckbox;
    @FXML private VBox expertSettingsPane;
    @FXML private TextField apiBaseUrlTextField;
    @FXML private SearchableComboBox<EmbeddingModelEnumeration> embeddingModelComboBox;
    @FXML private TextField temperatureTextField;
    @FXML private IntegerInputField contextWindowSizeTextField;
    @FXML private IntegerInputField documentSplitterChunkSizeTextField;
    @FXML private IntegerInputField documentSplitterOverlapSizeTextField;
    @FXML private IntegerInputField ragMaxResultsCountTextField;
    @FXML private TextField ragMinScoreTextField;
    @FXML private TabPane templatesTabPane;
    @FXML private Tab systemMessageForChattingTab;
    @FXML private Tab userMessageForChattingTab;
    @FXML private Tab summarizationChunkSystemMessageTab;
    @FXML private Tab summarizationChunkUserMessageTab;
    @FXML private Tab summarizationCombineSystemMessageTab;
    @FXML private Tab summarizationCombineUserMessageTab;
    @FXML private Tab citationParsingSystemMessageTab;
    @FXML private Tab citationParsingUserMessageTab;
    @FXML private Tab markdownChatExportTemplateTab;
    @FXML private TextArea systemMessageTextArea;
    @FXML private TextArea userMessageTextArea;
    @FXML private TextArea summarizationChunkSystemMessageTextArea;
    @FXML private TextArea summarizationChunkUserMessageTextArea;
    @FXML private TextArea summarizationCombineSystemMessageTextArea;
    @FXML private TextArea summarizationCombineUserMessageTextArea;
    @FXML private TextArea citationParsingSystemMessageTextArea;
    @FXML private TextArea citationParsingUserMessageTextArea;
    @FXML private TextArea markdownChatExportTemplateTextArea;
    @FXML private Button generalSettingsHelp;
    @FXML private Button expertSettingsHelp;
    @FXML private Button templatesHelp;
    @FXML private Button resetCurrentTemplateButton;
    @FXML private Button resetTemplatesButton;

    public AiTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new AiTabViewModel(preferences);

        initializeEnableAi();
        initializeAiProvider();
        initializeChatModel();
        initializeApiKey();
        initializeExpertSettings();
        initializeValidations();
        initializeTemplates();
        initializeHelp();
    }

    private void initializeHelp() {
        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_GENERAL_SETTINGS, dialogService, preferences.getExternalApplicationsPreferences()), generalSettingsHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_EXPERT_SETTINGS, dialogService, preferences.getExternalApplicationsPreferences()), expertSettingsHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_TEMPLATES, dialogService, preferences.getExternalApplicationsPreferences()), templatesHelp);
    }

    private void initializeTemplates() {
        systemMessageTextArea.textProperty().bindBidirectional(viewModel.chattingSystemMessageTemplateProperty());
        userMessageTextArea.textProperty().bindBidirectional(viewModel.chattingUserMessageTemplateProperty());
        summarizationChunkSystemMessageTextArea.textProperty().bindBidirectional(viewModel.summarizationChunkSystemMessageTemplateProperty());
        summarizationChunkUserMessageTextArea.textProperty().bindBidirectional(viewModel.summarizationChunkUserMessageTemplateProperty());
        summarizationCombineSystemMessageTextArea.textProperty().bindBidirectional(viewModel.summarizationCombineSystemMessageTemplateProperty());
        summarizationCombineUserMessageTextArea.textProperty().bindBidirectional(viewModel.summarizationCombineUserMessageTemplateProperty());
        citationParsingSystemMessageTextArea.textProperty().bindBidirectional(viewModel.citationParsingSystemMessageTemplateProperty());
        citationParsingUserMessageTextArea.textProperty().bindBidirectional(viewModel.citationParsingUserMessageTemplateProperty());
        markdownChatExportTemplateTextArea.textProperty().bindBidirectional(viewModel.markdownChatExportTemplateProperty());

        BooleanBinding aiDisabled = enableAi.selectedProperty().not();

        systemMessageTextArea.disableProperty().bind(aiDisabled);
        userMessageTextArea.disableProperty().bind(aiDisabled);
        summarizationChunkSystemMessageTextArea.disableProperty().bind(aiDisabled);
        summarizationChunkUserMessageTextArea.disableProperty().bind(aiDisabled);
        summarizationCombineSystemMessageTextArea.disableProperty().bind(aiDisabled);
        summarizationCombineUserMessageTextArea.disableProperty().bind(aiDisabled);
        citationParsingSystemMessageTextArea.disableProperty().bind(aiDisabled);
        citationParsingUserMessageTextArea.disableProperty().bind(aiDisabled);
        markdownChatExportTemplateTextArea.disableProperty().bind(aiDisabled);

        resetCurrentTemplateButton.disableProperty().bind(aiDisabled);
        resetTemplatesButton.disableProperty().bind(aiDisabled);
    }

    private void initializeValidations() {
        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.getApiTokenValidationStatus(), apiKeyTextField);
            visualizer.initVisualization(viewModel.getChatModelValidationStatus(), chatModelComboBox);
            visualizer.initVisualization(viewModel.getApiBaseUrlValidationStatus(), apiBaseUrlTextField);
            visualizer.initVisualization(viewModel.getEmbeddingModelValidationStatus(), embeddingModelComboBox);
            visualizer.initVisualization(viewModel.getTemperatureTypeValidationStatus(), temperatureTextField);
            visualizer.initVisualization(viewModel.getTemperatureRangeValidationStatus(), temperatureTextField);
            visualizer.initVisualization(viewModel.getMessageWindowSizeValidationStatus(), contextWindowSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterChunkSizeValidationStatus(), documentSplitterChunkSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterOverlapSizeValidationStatus(), documentSplitterOverlapSizeTextField);
            visualizer.initVisualization(viewModel.getRagMaxResultsCountValidationStatus(), ragMaxResultsCountTextField);
            visualizer.initVisualization(viewModel.getRagMinScoreTypeValidationStatus(), ragMinScoreTextField);
            visualizer.initVisualization(viewModel.getRagMinScoreRangeValidationStatus(), ragMinScoreTextField);
        });
    }

    private void initializeExpertSettings() {
        customizeExpertSettingsCheckbox.selectedProperty().bindBidirectional(viewModel.customizeExpertSettingsProperty());
        customizeExpertSettingsCheckbox.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        expertSettingsPane.visibleProperty().bind(customizeExpertSettingsCheckbox.selectedProperty());
        expertSettingsPane.managedProperty().bind(customizeExpertSettingsCheckbox.selectedProperty());

        new ViewModelListCellFactory<EmbeddingModelEnumeration>()
                .withText(EmbeddingModelEnumeration::fullInfo)
                .install(embeddingModelComboBox);
        embeddingModelComboBox.setItems(viewModel.embeddingModelsProperty());
        embeddingModelComboBox.valueProperty().bindBidirectional(viewModel.selectedEmbeddingModelProperty());
        embeddingModelComboBox.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        apiBaseUrlTextField.textProperty().bindBidirectional(viewModel.apiBaseUrlProperty());

        viewModel.disableExpertSettingsProperty().addListener((observable, oldValue, newValue) ->
                apiBaseUrlTextField.setDisable(newValue || viewModel.disableApiBaseUrlProperty().get())
        );

        viewModel.disableApiBaseUrlProperty().addListener((observable, oldValue, newValue) ->
                apiBaseUrlTextField.setDisable(newValue || viewModel.disableExpertSettingsProperty().get())
        );

        contextWindowSizeTextField.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.contextWindowSizeProperty().set(newValue == null ? 0 : newValue));

        viewModel.contextWindowSizeProperty().addListener((observable, oldValue, newValue) ->
                contextWindowSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        contextWindowSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        temperatureTextField.textProperty().bindBidirectional(viewModel.temperatureProperty());
        temperatureTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        documentSplitterChunkSizeTextField.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.documentSplitterChunkSizeProperty().set(newValue == null ? 0 : newValue));

        viewModel.documentSplitterChunkSizeProperty().addListener((observable, oldValue, newValue) ->
                documentSplitterChunkSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        documentSplitterChunkSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        documentSplitterOverlapSizeTextField.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.documentSplitterOverlapSizeProperty().set(newValue == null ? 0 : newValue));

        viewModel.documentSplitterOverlapSizeProperty().addListener((observable, oldValue, newValue) ->
                documentSplitterOverlapSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        documentSplitterOverlapSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        ragMaxResultsCountTextField.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.ragMaxResultsCountProperty().set(newValue == null ? 0 : newValue));

        viewModel.ragMaxResultsCountProperty().addListener((observable, oldValue, newValue) ->
                ragMaxResultsCountTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        ragMaxResultsCountTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        ragMinScoreTextField.textProperty().bindBidirectional(viewModel.ragMinScoreProperty());
        ragMinScoreTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());
    }

    private void initializeApiKey() {
        apiKeyTextField.textProperty().bindBidirectional(viewModel.apiKeyProperty());
        apiKeyTextField.disableProperty().bind(viewModel.disableBasicSettingsProperty());
    }

    private void initializeChatModel() {
        new ViewModelListCellFactory<String>()
                .withText(text -> text)
                .install(chatModelComboBox);
        chatModelComboBox.itemsProperty().bind(viewModel.chatModelsProperty());
        chatModelComboBox.valueProperty().bindBidirectional(viewModel.selectedChatModelProperty());
        chatModelComboBox.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        this.aiProviderComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == AiProvider.HUGGING_FACE) {
                chatModelComboBox.setPromptText(HUGGING_FACE_CHAT_MODEL_PROMPT);
            }
        });
    }

    private void initializeAiProvider() {
        new ViewModelListCellFactory<AiProvider>()
                .withText(AiProvider::toString)
                .install(aiProviderComboBox);
        aiProviderComboBox.itemsProperty().bind(viewModel.aiProvidersProperty());
        aiProviderComboBox.valueProperty().bindBidirectional(viewModel.selectedAiProviderProperty());
        aiProviderComboBox.disableProperty().bind(viewModel.disableBasicSettingsProperty());
    }

    private void initializeEnableAi() {
        enableAi.selectedProperty().bindBidirectional(viewModel.enableAi());
        autoGenerateSummaries.selectedProperty().bindBidirectional(viewModel.autoGenerateSummaries());
        autoGenerateSummaries.disableProperty().bind(
                Bindings.or(
                        enableAi.selectedProperty().not(),
                        viewModel.disableAutoGenerateSummaries()
                )
        );
        autoGenerateEmbeddings.selectedProperty().bindBidirectional(viewModel.autoGenerateEmbeddings());
        autoGenerateEmbeddings.disableProperty().bind(
                Bindings.or(
                        enableAi.selectedProperty().not(),
                        viewModel.disableAutoGenerateEmbeddings()
                )
        );
    }

    @Override
    public String getTabName() {
        return Localization.lang("AI");
    }

    @FXML
    private void onResetExpertSettingsButtonClick() {
        viewModel.resetExpertSettings();
    }

    @FXML
    private void onResetTemplatesButtonClick() {
        viewModel.resetTemplates();
    }

    @FXML
    private void onResetCurrentTemplateButtonClick() {
        Tab selectedTab = templatesTabPane.getSelectionModel().getSelectedItem();

        if (selectedTab == systemMessageForChattingTab) {
            viewModel.resetChattingSystemMessageTemplate();
        } else if (selectedTab == userMessageForChattingTab) {
            viewModel.resetChattingUserMessageTemplate();
        } else if (selectedTab == summarizationChunkSystemMessageTab) {
            viewModel.resetSummarizationChunkSystemMessageTemplate();
        } else if (selectedTab == summarizationCombineSystemMessageTab) {
            viewModel.resetSummarizationCombineSystemMessageTemplate();
        } else if (selectedTab == citationParsingSystemMessageTab) {
            viewModel.resetCitationParsingSystemMessageTemplate();
        } else if (selectedTab == citationParsingUserMessageTab) {
            viewModel.resetCitationParsingUserMessageTemplate();
        } else if (selectedTab == markdownChatExportTemplateTab) {
            viewModel.resetMarkdownChatExportTemplate();
        }
    }

    public ReadOnlyBooleanProperty aiEnabledProperty() {
        return enableAi.selectedProperty();
    }
}
