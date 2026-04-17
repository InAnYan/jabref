package org.jabref.gui.preferences.ai;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.ai.chatmodelconfig.ChatModelConfigView;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.embeddings.EmbeddingModelEnumeration;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.SearchableComboBox;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    @FXML private CheckBox enableAi;
    @FXML private CheckBox autoGenerateEmbeddings;
    @FXML private CheckBox autoGenerateSummaries;
    @FXML private ChatModelConfigView chatModelConfigView;
    @FXML private SearchableComboBox<EmbeddingModelEnumeration> embeddingModelComboBox;
    @FXML private IntegerInputField documentSplitterChunkSizeTextField;
    @FXML private IntegerInputField documentSplitterOverlapSizeTextField;
    @FXML private IntegerInputField ragMaxResultsCountTextField;
    @FXML private TextField ragMinScoreTextField;
    @FXML private TabPane templatesTabPane;
    @FXML private Tab systemMessageForChattingTab;
    @FXML private Tab userMessageForChattingTab;
    @FXML private Tab summarizationChunkSystemMessageTab;
    @FXML private Tab summarizationCombineSystemMessageTab;
    @FXML private Tab citationParsingSystemMessageTab;
    @FXML private Tab citationParsingUserMessageTab;
    @FXML private Tab markdownChatExportTemplateTab;
    @FXML private Tab followUpQuestionsTemplateTab;
    @FXML private TextArea systemMessageTextArea;
    @FXML private TextArea userMessageTextArea;
    @FXML private TextArea summarizationChunkSystemMessageTextArea;
    @FXML private TextArea summarizationChunkUserMessageTextArea;
    @FXML private TextArea summarizationCombineSystemMessageTextArea;
    @FXML private TextArea summarizationCombineUserMessageTextArea;
    @FXML private TextArea citationParsingSystemMessageTextArea;
    @FXML private TextArea citationParsingUserMessageTextArea;
    @FXML private TextArea markdownChatExportTemplateTextArea;
    @FXML private TextArea followUpQuestionsTemplateTextArea;
    @FXML private Button generalSettingsHelp;
    @FXML private Button expertSettingsHelp;
    @FXML private Button templatesHelp;
    @FXML private Button resetCurrentTemplateButton;
    @FXML private Button resetTemplatesButton;
    @FXML private CheckBox generateFollowUpQuestions;
    @FXML private Spinner<Integer> followUpQuestionsCountSpinner;

    public AiTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new AiTabViewModel(preferences);

        initializeEnableAi();
        initializeAdvancedSettings();
        initializeValidations();
        initializeTemplates();
        initializeFollowUpQuestions();
        initializeHelp();
    }

    @Override
    public void setValues() {
        super.setValues();
        chatModelConfigView.loadFrom(preferences.getAiPreferences());
    }

    @Override
    public void storeSettings() {
        chatModelConfigView.storeInto(preferences.getAiPreferences());
        super.storeSettings();
    }

    @Override
    public boolean validateSettings() {
        if (viewModel.enableAi().get()) {
            if (!chatModelConfigView.getViewModel().validate()) {
                return false;
            }
        }
        return super.validateSettings();
    }

    private void initializeFollowUpQuestions() {
        generateFollowUpQuestions.selectedProperty().bindBidirectional(viewModel.generateFollowUpQuestionsProperty());
        generateFollowUpQuestions.disableProperty().bind(viewModel.disableSettingsProperty());

        followUpQuestionsCountSpinner.setValueFactory(AiTabViewModel.followUpQuestionsCountValueFactory);
        followUpQuestionsCountSpinner.getValueFactory().valueProperty().bindBidirectional(viewModel.followUpQuestionsCountProperty().asObject());
        followUpQuestionsCountSpinner.disableProperty().bind(generateFollowUpQuestions.selectedProperty().not());
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
        followUpQuestionsTemplateTextArea.textProperty().bindBidirectional(viewModel.followUpQuestionsTemplateProperty());

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
        followUpQuestionsTemplateTextArea.disableProperty().bind(aiDisabled);

        resetCurrentTemplateButton.disableProperty().bind(aiDisabled);
        resetTemplatesButton.disableProperty().bind(aiDisabled);
    }

    private void initializeValidations() {
        visualizer.initVisualization(viewModel.getEmbeddingModelValidationStatus(), embeddingModelComboBox);
        visualizer.initVisualization(viewModel.getDocumentSplitterChunkSizeValidationStatus(), documentSplitterChunkSizeTextField);
        visualizer.initVisualization(viewModel.getDocumentSplitterOverlapSizeValidationStatus(), documentSplitterOverlapSizeTextField);
        visualizer.initVisualization(viewModel.getRagMaxResultsCountValidationStatus(), ragMaxResultsCountTextField);
        visualizer.initVisualization(viewModel.getRagMinScoreTypeValidationStatus(), ragMinScoreTextField);
        visualizer.initVisualization(viewModel.getRagMinScoreRangeValidationStatus(), ragMinScoreTextField);
    }

    private void initializeAdvancedSettings() {
        new ViewModelListCellFactory<EmbeddingModelEnumeration>()
                .withText(EmbeddingModelEnumeration::fullInfo)
                .install(embeddingModelComboBox);
        embeddingModelComboBox.setItems(viewModel.embeddingModelsProperty());
        embeddingModelComboBox.valueProperty().bindBidirectional(viewModel.selectedEmbeddingModelProperty());
        embeddingModelComboBox.disableProperty().bind(enableAi.selectedProperty().not());

        documentSplitterChunkSizeTextField.valueProperty().addListener((_, _, newValue) ->
                viewModel.documentSplitterChunkSizeProperty().set(newValue == null ? 0 : newValue));

        viewModel.documentSplitterChunkSizeProperty().addListener((_, _, newValue) ->
                documentSplitterChunkSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        documentSplitterChunkSizeTextField.disableProperty().bind(enableAi.selectedProperty().not());

        documentSplitterOverlapSizeTextField.valueProperty().addListener((_, _, newValue) ->
                viewModel.documentSplitterOverlapSizeProperty().set(newValue == null ? 0 : newValue));

        viewModel.documentSplitterOverlapSizeProperty().addListener((_, _, newValue) ->
                documentSplitterOverlapSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        documentSplitterOverlapSizeTextField.disableProperty().bind(enableAi.selectedProperty().not());

        ragMaxResultsCountTextField.valueProperty().addListener((_, _, newValue) ->
                viewModel.ragMaxResultsCountProperty().set(newValue == null ? 0 : newValue));

        viewModel.ragMaxResultsCountProperty().addListener((_, _, newValue) ->
                ragMaxResultsCountTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        ragMaxResultsCountTextField.disableProperty().bind(enableAi.selectedProperty().not());

        ragMinScoreTextField.textProperty().bindBidirectional(viewModel.ragMinScoreProperty());
        ragMinScoreTextField.disableProperty().bind(enableAi.selectedProperty().not());
    }

    private void initializeEnableAi() {
        enableAi.selectedProperty().bindBidirectional(viewModel.enableAi());

        autoGenerateSummaries.selectedProperty().bindBidirectional(viewModel.autoGenerateSummaries());
        autoGenerateSummaries.disableProperty().bind(enableAi.selectedProperty().not());

        autoGenerateEmbeddings.selectedProperty().bindBidirectional(viewModel.autoGenerateEmbeddings());
        autoGenerateEmbeddings.disableProperty().bind(enableAi.selectedProperty().not());
    }

    @Override
    public String getTabName() {
        return Localization.lang("AI");
    }

    @FXML
    private void onResetAdvancedSettingsButtonClick() {
        chatModelConfigView.resetToDefaults();
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
        } else if (selectedTab == followUpQuestionsTemplateTab) {
            viewModel.resetFollowUpQuestionsTemplate();
        }
    }

    public ReadOnlyBooleanProperty aiEnabledProperty() {
        return enableAi.selectedProperty();
    }
}
