package org.jabref.gui.preferences.ai;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.ai.preferences.AiDefaultTemplates;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.LocalizedNumbers;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.embeddings.EmbeddingModelEnumeration;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.llm.PredefinedChatModel;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class AiTabViewModel implements PreferenceTabViewModel {
    private final Locale oldLocale;

    private final BooleanProperty enableAi = new SimpleBooleanProperty();
    private final BooleanProperty autoGenerateEmbeddings = new SimpleBooleanProperty();
    private final BooleanProperty disableAutoGenerateEmbeddings = new SimpleBooleanProperty();
    private final BooleanProperty autoGenerateSummaries = new SimpleBooleanProperty();
    private final BooleanProperty disableAutoGenerateSummaries = new SimpleBooleanProperty();

    private final ListProperty<AiProvider> aiProvidersList =
            new SimpleListProperty<>(FXCollections.observableArrayList(AiProvider.values()));
    private final ObjectProperty<AiProvider> selectedAiProvider = new SimpleObjectProperty<>();

    private final ListProperty<String> chatModelsList =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    private final StringProperty currentChatModel = new SimpleStringProperty();

    private final StringProperty openAiChatModel = new SimpleStringProperty();
    private final StringProperty mistralAiChatModel = new SimpleStringProperty();
    private final StringProperty geminiChatModel = new SimpleStringProperty();
    private final StringProperty huggingFaceChatModel = new SimpleStringProperty();

    private final StringProperty currentApiKey = new SimpleStringProperty();

    private final StringProperty openAiApiKey = new SimpleStringProperty();
    private final StringProperty mistralAiApiKey = new SimpleStringProperty();
    private final StringProperty geminiAiApiKey = new SimpleStringProperty();
    private final StringProperty huggingFaceApiKey = new SimpleStringProperty();

    private final BooleanProperty customizeExpertSettings = new SimpleBooleanProperty();

    private final ListProperty<EmbeddingModelEnumeration> embeddingModelsList =
            new SimpleListProperty<>(FXCollections.observableArrayList(EmbeddingModelEnumeration.values()));
    private final ObjectProperty<EmbeddingModelEnumeration> selectedEmbeddingModel = new SimpleObjectProperty<>();

    private final StringProperty currentApiBaseUrl = new SimpleStringProperty();
    private final BooleanProperty disableApiBaseUrl = new SimpleBooleanProperty(true); // {@link HuggingFaceChatModel} and {@link GoogleAiGeminiChatModel} doesn't support setting API base URL

    private final StringProperty openAiApiBaseUrl = new SimpleStringProperty();
    private final StringProperty mistralAiApiBaseUrl = new SimpleStringProperty();
    private final StringProperty geminiApiBaseUrl = new SimpleStringProperty();
    private final StringProperty huggingFaceApiBaseUrl = new SimpleStringProperty();

    private final StringProperty chattingSystemMessageTemplate = new SimpleStringProperty();
    private final StringProperty chattingUserMessageTemplate = new SimpleStringProperty();
    private final StringProperty summarizationChunkSystemMessageTemplate = new SimpleStringProperty();
    private final StringProperty summarizationChunkUserMessageTemplate = new SimpleStringProperty();
    private final StringProperty summarizationCombineSystemMessageTemplate = new SimpleStringProperty();
    private final StringProperty summarizationCombineUserMessageTemplate = new SimpleStringProperty();
    private final StringProperty citationParsingSystemMessageTemplate = new SimpleStringProperty();
    private final StringProperty citationParsingUserMessageTemplate = new SimpleStringProperty();
    private final StringProperty summarizationFullDocumentSystemMessageTemplate = new SimpleStringProperty();
    private final StringProperty summarizationFullDocumentUserMessageTemplate = new SimpleStringProperty();
    private final StringProperty markdownChatExportTemplate = new SimpleStringProperty();

    private final StringProperty temperature = new SimpleStringProperty();
    private final IntegerProperty contextWindowSize = new SimpleIntegerProperty();
    private final IntegerProperty documentSplitterChunkSize = new SimpleIntegerProperty();
    private final IntegerProperty documentSplitterOverlapSize = new SimpleIntegerProperty();
    private final IntegerProperty ragMaxResultsCount = new SimpleIntegerProperty();
    private final StringProperty ragMinScore = new SimpleStringProperty();

    private final BooleanProperty disableBasicSettings = new SimpleBooleanProperty(true);
    private final BooleanProperty disableExpertSettings = new SimpleBooleanProperty(true);

    private final AiPreferences aiPreferences;

    private final Validator apiKeyValidator;
    private final Validator chatModelValidator;
    private final Validator apiBaseUrlValidator;
    private final Validator embeddingModelValidator;
    private final Validator temperatureTypeValidator;
    private final Validator temperatureRangeValidator;
    private final Validator contextWindowSizeValidator;
    private final Validator documentSplitterChunkSizeValidator;
    private final Validator documentSplitterOverlapSizeValidator;
    private final Validator ragMaxResultsCountValidator;
    private final Validator ragMinScoreTypeValidator;
    private final Validator ragMinScoreRangeValidator;

    public AiTabViewModel(CliPreferences preferences) {
        this.oldLocale = Locale.getDefault();

        this.aiPreferences = preferences.getAiPreferences();

        this.enableAi.addListener((_, _, newValue) -> {
            disableBasicSettings.set(!newValue);
            disableExpertSettings.set(!newValue || !customizeExpertSettings.get());
        });

        this.customizeExpertSettings.addListener((_, _, newValue) ->
                disableExpertSettings.set(!newValue || !enableAi.get())
        );

        this.selectedAiProvider.addListener((_, oldValue, newValue) -> {
            List<String> models = PredefinedChatModel.getAvailableModels(newValue);

            disableApiBaseUrl.set(newValue == AiProvider.HUGGING_FACE || newValue == AiProvider.GEMINI);

            // When we setAll on Hugging Face, models are empty, and currentChatModel become null.
            // It becomes null because currentChatModel is bound to combobox, and this combobox becomes empty.
            // For some reason, custom edited value in the combobox will be erased, so we need to store the old value.
            String oldChatModel = currentChatModel.get();
            chatModelsList.setAll(models);

            if (oldValue != null) {
                switch (oldValue) {
                    case OPEN_AI -> {
                        openAiChatModel.set(oldChatModel);
                        openAiApiKey.set(currentApiKey.get());
                        openAiApiBaseUrl.set(currentApiBaseUrl.get());
                    }
                    case MISTRAL_AI -> {
                        mistralAiChatModel.set(oldChatModel);
                        mistralAiApiKey.set(currentApiKey.get());
                        mistralAiApiBaseUrl.set(currentApiBaseUrl.get());
                    }
                    case GEMINI -> {
                        geminiChatModel.set(oldChatModel);
                        geminiAiApiKey.set(currentApiKey.get());
                        geminiApiBaseUrl.set(currentApiBaseUrl.get());
                    }
                    case HUGGING_FACE -> {
                        huggingFaceChatModel.set(oldChatModel);
                        huggingFaceApiKey.set(currentApiKey.get());
                        huggingFaceApiBaseUrl.set(currentApiBaseUrl.get());
                    }
                }
            }

            switch (newValue) {
                case OPEN_AI -> {
                    currentChatModel.set(openAiChatModel.get());
                    currentApiKey.set(openAiApiKey.get());
                    currentApiBaseUrl.set(openAiApiBaseUrl.get());
                }
                case MISTRAL_AI -> {
                    currentChatModel.set(mistralAiChatModel.get());
                    currentApiKey.set(mistralAiApiKey.get());
                    currentApiBaseUrl.set(mistralAiApiBaseUrl.get());
                }
                case GEMINI -> {
                    currentChatModel.set(geminiChatModel.get());
                    currentApiKey.set(geminiAiApiKey.get());
                    currentApiBaseUrl.set(geminiApiBaseUrl.get());
                }
                case HUGGING_FACE -> {
                    currentChatModel.set(huggingFaceChatModel.get());
                    currentApiKey.set(huggingFaceApiKey.get());
                    currentApiBaseUrl.set(huggingFaceApiBaseUrl.get());
                }
            }
        });

        this.currentChatModel.addListener((_, _, newValue) -> {
            if (newValue == null) {
                return;
            }

            switch (selectedAiProvider.get()) {
                case OPEN_AI ->
                        openAiChatModel.set(newValue);
                case MISTRAL_AI ->
                        mistralAiChatModel.set(newValue);
                case GEMINI ->
                        geminiChatModel.set(newValue);
                case HUGGING_FACE ->
                        huggingFaceChatModel.set(newValue);
            }

            contextWindowSize.set(PredefinedChatModel.getContextWindowSize(selectedAiProvider.get(), newValue));
        });

        this.currentApiKey.addListener((_, _, newValue) -> {
            switch (selectedAiProvider.get()) {
                case OPEN_AI ->
                        openAiApiKey.set(newValue);
                case MISTRAL_AI ->
                        mistralAiApiKey.set(newValue);
                case GEMINI ->
                        geminiAiApiKey.set(newValue);
                case HUGGING_FACE ->
                        huggingFaceApiKey.set(newValue);
            }
        });

        this.currentApiBaseUrl.addListener((_, _, newValue) -> {
            switch (selectedAiProvider.get()) {
                case OPEN_AI ->
                        openAiApiBaseUrl.set(newValue);
                case MISTRAL_AI ->
                        mistralAiApiBaseUrl.set(newValue);
                case GEMINI ->
                        geminiApiBaseUrl.set(newValue);
                case HUGGING_FACE ->
                        huggingFaceApiBaseUrl.set(newValue);
            }
        });

        this.apiKeyValidator = new FunctionBasedValidator<>(
                currentApiKey,
                token -> !StringUtil.isBlank(token),
                ValidationMessage.error(Localization.lang("An API key has to be provided")));

        this.chatModelValidator = new FunctionBasedValidator<>(
                currentChatModel,
                chatModel -> !StringUtil.isBlank(chatModel),
                ValidationMessage.error(Localization.lang("Chat model has to be provided")));

        this.apiBaseUrlValidator = new FunctionBasedValidator<>(
                currentApiBaseUrl,
                token -> !StringUtil.isBlank(token),
                ValidationMessage.error(Localization.lang("API base URL has to be provided")));

        this.embeddingModelValidator = new FunctionBasedValidator<>(
                selectedEmbeddingModel,
                Objects::nonNull,
                ValidationMessage.error(Localization.lang("Embedding model has to be provided")));

        this.temperatureTypeValidator = new FunctionBasedValidator<>(
                temperature,
                temp -> LocalizedNumbers.stringToDouble(temp).isPresent(),
                ValidationMessage.error(Localization.lang("Temperature must be a number")));

        // Source: https://platform.openai.com/docs/api-reference/chat/create#chat-create-temperature
        this.temperatureRangeValidator = new FunctionBasedValidator<>(
                temperature,
                temp -> LocalizedNumbers.stringToDouble(temp).map(t -> t >= 0 && t <= 2).orElse(false),
                ValidationMessage.error(Localization.lang("Temperature must be between 0 and 2")));

        this.contextWindowSizeValidator = new FunctionBasedValidator<>(
                contextWindowSize,
                size -> size.intValue() > 0,
                ValidationMessage.error(Localization.lang("Context window size must be greater than 0")));

        this.documentSplitterChunkSizeValidator = new FunctionBasedValidator<>(
                documentSplitterChunkSize,
                size -> size.intValue() > 0,
                ValidationMessage.error(Localization.lang("Document splitter chunk size must be greater than 0")));

        this.documentSplitterOverlapSizeValidator = new FunctionBasedValidator<>(
                documentSplitterOverlapSize,
                size -> size.intValue() > 0 && size.intValue() < documentSplitterChunkSize.get(),
                ValidationMessage.error(Localization.lang("Document splitter overlap size must be greater than 0 and less than chunk size")));

        this.ragMaxResultsCountValidator = new FunctionBasedValidator<>(
                ragMaxResultsCount,
                count -> count.intValue() > 0,
                ValidationMessage.error(Localization.lang("RAG max results count must be greater than 0")));

        this.ragMinScoreTypeValidator = new FunctionBasedValidator<>(
                ragMinScore,
                minScore -> LocalizedNumbers.stringToDouble(minScore).isPresent(),
                ValidationMessage.error(Localization.lang("RAG minimum score must be a number")));

        this.ragMinScoreRangeValidator = new FunctionBasedValidator<>(
                ragMinScore,
                minScore -> LocalizedNumbers.stringToDouble(minScore).map(s -> s > 0 && s < 1).orElse(false),
                ValidationMessage.error(Localization.lang("RAG minimum score must be greater than 0 and less than 1")));
    }

    @Override
    public void setValues() {
        openAiApiKey.setValue(aiPreferences.getApiKeyForAiProvider(AiProvider.OPEN_AI));
        mistralAiApiKey.setValue(aiPreferences.getApiKeyForAiProvider(AiProvider.MISTRAL_AI));
        geminiAiApiKey.setValue(aiPreferences.getApiKeyForAiProvider(AiProvider.GEMINI));
        huggingFaceApiKey.setValue(aiPreferences.getApiKeyForAiProvider(AiProvider.HUGGING_FACE));

        openAiApiBaseUrl.setValue(aiPreferences.getOpenAiApiBaseUrl());
        mistralAiApiBaseUrl.setValue(aiPreferences.getMistralAiApiBaseUrl());
        geminiApiBaseUrl.setValue(aiPreferences.getGeminiApiBaseUrl());
        huggingFaceApiBaseUrl.setValue(aiPreferences.getHuggingFaceApiBaseUrl());

        openAiChatModel.setValue(aiPreferences.getOpenAiChatModel());
        mistralAiChatModel.setValue(aiPreferences.getMistralAiChatModel());
        geminiChatModel.setValue(aiPreferences.getGeminiChatModel());
        huggingFaceChatModel.setValue(aiPreferences.getHuggingFaceChatModel());

        enableAi.setValue(aiPreferences.getEnableAi());
        autoGenerateSummaries.setValue(aiPreferences.getAutoGenerateSummaries());
        autoGenerateEmbeddings.setValue(aiPreferences.getAutoGenerateEmbeddings());

        selectedAiProvider.setValue(aiPreferences.getAiProvider());

        customizeExpertSettings.setValue(aiPreferences.getCustomizeExpertSettings());

        selectedEmbeddingModel.setValue(aiPreferences.getEmbeddingModel());

        chattingSystemMessageTemplate.set(aiPreferences.getChattingSystemMessageTemplate());
        chattingUserMessageTemplate.set(aiPreferences.getChattingUserMessageTemplate());
        summarizationChunkSystemMessageTemplate.set(aiPreferences.getSummarizationChunkSystemMessageTemplate());
        summarizationChunkUserMessageTemplate.set(aiPreferences.getSummarizationChunkUserMessageTemplate());
        summarizationCombineSystemMessageTemplate.set(aiPreferences.getSummarizationCombineSystemMessageTemplate());
        summarizationCombineUserMessageTemplate.set(aiPreferences.getSummarizationCombineUserMessageTemplate());
        citationParsingSystemMessageTemplate.set(aiPreferences.getCitationParsingSystemMessageTemplate());
        citationParsingUserMessageTemplate.set(aiPreferences.getCitationParsingUserMessageTemplate());
        summarizationFullDocumentSystemMessageTemplate.set(aiPreferences.getSummarizationFullDocumentSystemMessageTemplate());
        summarizationFullDocumentUserMessageTemplate.set(aiPreferences.getSummarizationFullDocumentUserMessageTemplate());
        markdownChatExportTemplate.set(aiPreferences.getMarkdownChatExportTemplate());

        temperature.setValue(LocalizedNumbers.doubleToString(aiPreferences.getTemperature()));
        contextWindowSize.setValue(aiPreferences.getContextWindowSize());
        documentSplitterChunkSize.setValue(aiPreferences.getDocumentSplitterChunkSize());
        documentSplitterOverlapSize.setValue(aiPreferences.getDocumentSplitterOverlapSize());
        ragMaxResultsCount.setValue(aiPreferences.getRagMaxResultsCount());
        ragMinScore.setValue(LocalizedNumbers.doubleToString(aiPreferences.getRagMinScore()));
    }

    @Override
    public void storeSettings() {
        aiPreferences.setEnableAi(enableAi.get());
        aiPreferences.setAutoGenerateEmbeddings(autoGenerateEmbeddings.get());
        aiPreferences.setAutoGenerateSummaries(autoGenerateSummaries.get());

        aiPreferences.setAiProvider(selectedAiProvider.get());

        aiPreferences.setOpenAiChatModel(openAiChatModel.get() == null ? "" : openAiChatModel.get());
        aiPreferences.setMistralAiChatModel(mistralAiChatModel.get() == null ? "" : mistralAiChatModel.get());
        aiPreferences.setGeminiChatModel(geminiChatModel.get() == null ? "" : geminiChatModel.get());
        aiPreferences.setHuggingFaceChatModel(huggingFaceChatModel.get() == null ? "" : huggingFaceChatModel.get());

        aiPreferences.storeAiApiKeyInKeyring(AiProvider.OPEN_AI, openAiApiKey.get() == null ? "" : openAiApiKey.get());
        aiPreferences.storeAiApiKeyInKeyring(AiProvider.MISTRAL_AI, mistralAiApiKey.get() == null ? "" : mistralAiApiKey.get());
        aiPreferences.storeAiApiKeyInKeyring(AiProvider.GEMINI, geminiAiApiKey.get() == null ? "" : geminiAiApiKey.get());
        aiPreferences.storeAiApiKeyInKeyring(AiProvider.HUGGING_FACE, huggingFaceApiKey.get() == null ? "" : huggingFaceApiKey.get());
        // We notify in all cases without a real check if something was changed
        aiPreferences.apiKeyUpdated();

        aiPreferences.setCustomizeExpertSettings(customizeExpertSettings.get());

        aiPreferences.setEmbeddingModel(selectedEmbeddingModel.get());

        aiPreferences.setOpenAiApiBaseUrl(openAiApiBaseUrl.get() == null ? "" : openAiApiBaseUrl.get());
        aiPreferences.setMistralAiApiBaseUrl(mistralAiApiBaseUrl.get() == null ? "" : mistralAiApiBaseUrl.get());
        aiPreferences.setGeminiApiBaseUrl(geminiApiBaseUrl.get() == null ? "" : geminiApiBaseUrl.get());
        aiPreferences.setHuggingFaceApiBaseUrl(huggingFaceApiBaseUrl.get() == null ? "" : huggingFaceApiBaseUrl.get());

        aiPreferences.setChattingSystemMessageTemplate(chattingSystemMessageTemplate.get());
        aiPreferences.setChattingUserMessageTemplate(chattingUserMessageTemplate.get());
        aiPreferences.setSummarizationChunkSystemMessageTemplate(summarizationChunkSystemMessageTemplate.get());
        aiPreferences.setSummarizationChunkUserMessageTemplate(summarizationChunkUserMessageTemplate.get());
        aiPreferences.setSummarizationCombineSystemMessageTemplate(summarizationCombineSystemMessageTemplate.get());
        aiPreferences.setSummarizationCombineUserMessageTemplate(summarizationCombineUserMessageTemplate.get());
        aiPreferences.setCitationParsingSystemMessageTemplate(citationParsingSystemMessageTemplate.get());
        aiPreferences.setCitationParsingUserMessageTemplate(citationParsingUserMessageTemplate.get());
        aiPreferences.setSummarizationFullDocumentSystemMessageTemplate(summarizationFullDocumentSystemMessageTemplate.get());
        aiPreferences.setSummarizationFullDocumentUserMessageTemplate(summarizationFullDocumentUserMessageTemplate.get());
        aiPreferences.setMarkdownChatExportTemplate(markdownChatExportTemplate.get());

        // We already check the correctness of temperature and RAG minimum score in validators, so we don't need to check it here.
        aiPreferences.setTemperature(LocalizedNumbers.stringToDouble(oldLocale, temperature.get()).get());
        aiPreferences.setContextWindowSize(contextWindowSize.get());
        aiPreferences.setDocumentSplitterChunkSize(documentSplitterChunkSize.get());
        aiPreferences.setDocumentSplitterOverlapSize(documentSplitterOverlapSize.get());
        aiPreferences.setRagMaxResultsCount(ragMaxResultsCount.get());
        aiPreferences.setRagMinScore(LocalizedNumbers.stringToDouble(oldLocale, ragMinScore.get()).get());
    }

    public void resetExpertSettings() {
        String resetApiBaseUrl = selectedAiProvider.get().getApiUrl();
        currentApiBaseUrl.set(resetApiBaseUrl);

        contextWindowSize.set(PredefinedChatModel.getContextWindowSize(selectedAiProvider.get(), currentChatModel.get()));

        // TODO: Get default values.
        temperature.set(LocalizedNumbers.doubleToString(0.7));
        documentSplitterChunkSize.set(300);
        documentSplitterOverlapSize.set(100);
        ragMaxResultsCount.set(10);
        ragMinScore.set(LocalizedNumbers.doubleToString(0.3));
    }

    public void resetTemplates() {
        resetChattingSystemMessageTemplate();
        resetChattingUserMessageTemplate();
        resetSummarizationChunkSystemMessageTemplate();
        resetSummarizationCombineSystemMessageTemplate();
        resetCitationParsingSystemMessageTemplate();
        resetCitationParsingUserMessageTemplate();
        resetSummarizationFullDocumentSystemMessageTemplate();
        resetMarkdownChatExportTemplate();
    }

    public void resetChattingSystemMessageTemplate() {
        chattingSystemMessageTemplate.set(AiDefaultTemplates.getChattingSystemMessageTemplate());
    }

    public void resetChattingUserMessageTemplate() {
        chattingUserMessageTemplate.set(AiDefaultTemplates.getChattingUserMessageTemplate());
    }

    public void resetSummarizationChunkSystemMessageTemplate() {
        summarizationChunkSystemMessageTemplate.set(AiDefaultTemplates.getSummarizationChunkSystemMessageTemplate());
    }

    public void resetSummarizationCombineSystemMessageTemplate() {
        summarizationCombineSystemMessageTemplate.set(AiDefaultTemplates.getSummarizationCombineSystemMessageTemplate());
    }

    public void resetCitationParsingSystemMessageTemplate() {
        citationParsingSystemMessageTemplate.set(AiDefaultTemplates.getCitationParsingSystemMessageTemplate());
    }

    public void resetCitationParsingUserMessageTemplate() {
        citationParsingUserMessageTemplate.set(AiDefaultTemplates.getCitationParsingUserMessageTemplate());
    }

    public void resetSummarizationFullDocumentSystemMessageTemplate() {
        summarizationFullDocumentSystemMessageTemplate.set(AiDefaultTemplates.getSummarizationFullDocumentSystemMessageTemplate());
    }

    public void resetMarkdownChatExportTemplate() {
        markdownChatExportTemplate.set(AiDefaultTemplates.getMarkdownChatExportTemplate());
    }

    public StringProperty markdownChatExportTemplateProperty() {
        return markdownChatExportTemplate;
    }

    @Override
    public boolean validateSettings() {
        if (enableAi.get()) {
            if (customizeExpertSettings.get()) {
                return validateBasicSettings() && validateExpertSettings();
            } else {
                return validateBasicSettings();
            }
        }

        return true;
    }

    public boolean validateBasicSettings() {
        List<Validator> validators = List.of(
                chatModelValidator
                // apiKeyValidator -- skipped, it will generate warning, but the preferences should be able to save.
        );

        return validators.stream().map(Validator::getValidationStatus).allMatch(ValidationStatus::isValid);
    }

    public boolean validateExpertSettings() {
        List<Validator> validators = List.of(
                apiBaseUrlValidator,
                embeddingModelValidator,
                temperatureTypeValidator,
                temperatureRangeValidator,
                contextWindowSizeValidator,
                documentSplitterChunkSizeValidator,
                documentSplitterOverlapSizeValidator,
                ragMaxResultsCountValidator,
                ragMinScoreTypeValidator,
                ragMinScoreRangeValidator
        );

        return validators.stream().map(Validator::getValidationStatus).allMatch(ValidationStatus::isValid);
    }

    public BooleanProperty enableAi() {
        return enableAi;
    }

    public BooleanProperty autoGenerateEmbeddings() {
        return autoGenerateEmbeddings;
    }

    public BooleanProperty disableAutoGenerateEmbeddings() {
        return disableAutoGenerateEmbeddings;
    }

    public BooleanProperty autoGenerateSummaries() {
        return autoGenerateSummaries;
    }

    public BooleanProperty disableAutoGenerateSummaries() {
        return disableAutoGenerateSummaries;
    }

    public ReadOnlyListProperty<AiProvider> aiProvidersProperty() {
        return aiProvidersList;
    }

    public ObjectProperty<AiProvider> selectedAiProviderProperty() {
        return selectedAiProvider;
    }

    public ReadOnlyListProperty<String> chatModelsProperty() {
        return chatModelsList;
    }

    public StringProperty selectedChatModelProperty() {
        return currentChatModel;
    }

    public StringProperty apiKeyProperty() {
        return currentApiKey;
    }

    public BooleanProperty customizeExpertSettingsProperty() {
        return customizeExpertSettings;
    }

    public ReadOnlyListProperty<EmbeddingModelEnumeration> embeddingModelsProperty() {
        return embeddingModelsList;
    }

    public ObjectProperty<EmbeddingModelEnumeration> selectedEmbeddingModelProperty() {
        return selectedEmbeddingModel;
    }

    public StringProperty apiBaseUrlProperty() {
        return currentApiBaseUrl;
    }

    public BooleanProperty disableApiBaseUrlProperty() {
        return disableApiBaseUrl;
    }

    public StringProperty chattingSystemMessageTemplateProperty() {
        return chattingSystemMessageTemplate;
    }

    public StringProperty chattingUserMessageTemplateProperty() {
        return chattingUserMessageTemplate;
    }

    public StringProperty summarizationChunkSystemMessageTemplateProperty() {
        return summarizationChunkSystemMessageTemplate;
    }

    public StringProperty summarizationChunkUserMessageTemplateProperty() {
        return summarizationChunkUserMessageTemplate;
    }

    public StringProperty summarizationCombineSystemMessageTemplateProperty() {
        return summarizationCombineSystemMessageTemplate;
    }

    public StringProperty summarizationCombineUserMessageTemplateProperty() {
        return summarizationCombineUserMessageTemplate;
    }

    public StringProperty citationParsingSystemMessageTemplateProperty() {
        return citationParsingSystemMessageTemplate;
    }

    public StringProperty citationParsingUserMessageTemplateProperty() {
        return citationParsingUserMessageTemplate;
    }

    public StringProperty temperatureProperty() {
        return temperature;
    }

    public IntegerProperty contextWindowSizeProperty() {
        return contextWindowSize;
    }

    public IntegerProperty documentSplitterChunkSizeProperty() {
        return documentSplitterChunkSize;
    }

    public IntegerProperty documentSplitterOverlapSizeProperty() {
        return documentSplitterOverlapSize;
    }

    public IntegerProperty ragMaxResultsCountProperty() {
        return ragMaxResultsCount;
    }

    public StringProperty ragMinScoreProperty() {
        return ragMinScore;
    }

    public BooleanProperty disableBasicSettingsProperty() {
        return disableBasicSettings;
    }

    public BooleanProperty disableExpertSettingsProperty() {
        return disableExpertSettings;
    }

    public ValidationStatus getApiTokenValidationStatus() {
        return apiKeyValidator.getValidationStatus();
    }

    public ValidationStatus getChatModelValidationStatus() {
        return chatModelValidator.getValidationStatus();
    }

    public ValidationStatus getApiBaseUrlValidationStatus() {
        return apiBaseUrlValidator.getValidationStatus();
    }

    public ValidationStatus getEmbeddingModelValidationStatus() {
        return embeddingModelValidator.getValidationStatus();
    }

    public ValidationStatus getTemperatureTypeValidationStatus() {
        return temperatureTypeValidator.getValidationStatus();
    }

    public ValidationStatus getTemperatureRangeValidationStatus() {
        return temperatureRangeValidator.getValidationStatus();
    }

    public ValidationStatus getMessageWindowSizeValidationStatus() {
        return contextWindowSizeValidator.getValidationStatus();
    }

    public ValidationStatus getDocumentSplitterChunkSizeValidationStatus() {
        return documentSplitterChunkSizeValidator.getValidationStatus();
    }

    public ValidationStatus getDocumentSplitterOverlapSizeValidationStatus() {
        return documentSplitterOverlapSizeValidator.getValidationStatus();
    }

    public ValidationStatus getRagMaxResultsCountValidationStatus() {
        return ragMaxResultsCountValidator.getValidationStatus();
    }

    public ValidationStatus getRagMinScoreTypeValidationStatus() {
        return ragMinScoreTypeValidator.getValidationStatus();
    }

    public ValidationStatus getRagMinScoreRangeValidationStatus() {
        return ragMinScoreRangeValidator.getValidationStatus();
    }
}
