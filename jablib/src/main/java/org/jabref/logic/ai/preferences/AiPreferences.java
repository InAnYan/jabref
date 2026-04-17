package org.jabref.logic.ai.preferences;

import java.util.List;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.llm.PredefinedChatModel;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiPreferences.class);

    private static final String KEYRING_AI_SERVICE = "org.jabref.ai";
    private static final String KEYRING_AI_SERVICE_ACCOUNT = "apiKey";

    private final BooleanProperty enableAi;
    private final BooleanProperty autoGenerateEmbeddings;
    private final BooleanProperty autoGenerateSummaries;

    private final ObjectProperty<AiProvider> aiProvider;

    // TODO: Add chat model property.

    private final StringProperty openAiChatModel;
    private final StringProperty mistralAiChatModel;
    private final StringProperty geminiChatModel;
    private final StringProperty huggingFaceChatModel;

    private final BooleanProperty customizeExpertSettings;

    private final StringProperty openAiApiBaseUrl;
    private final StringProperty mistralAiApiBaseUrl;
    private final StringProperty geminiApiBaseUrl;
    private final StringProperty huggingFaceApiBaseUrl;

    private final ObjectProperty<SummarizatorKind> summarizatorKind;
    private final ObjectProperty<TokenEstimatorKind> tokenEstimatorKind;
    private final ObjectProperty<PredefinedEmbeddingModel> embeddingModel;
    private final DoubleProperty temperature;
    private final IntegerProperty contextWindowSize;

    private final ObjectProperty<DocumentSplitterKind> documentSplitterKind;
    private final IntegerProperty documentSplitterChunkSize;
    private final IntegerProperty documentSplitterOverlapSize;

    private final ObjectProperty<AnswerEngineKind> answerEngineKind;
    private final IntegerProperty ragMaxResultsCount;
    private final DoubleProperty ragMinScore;

    private final StringProperty chattingSystemMessageTemplate;
    private final StringProperty chattingUserMessageTemplate;
    private final StringProperty summarizationChunkSystemMessageTemplate;
    private final StringProperty summarizationChunkUserMessageTemplate;
    private final StringProperty summarizationCombineSystemMessageTemplate;
    private final StringProperty summarizationCombineUserMessageTemplate;
    private final StringProperty summarizationFullDocumentSystemMessageTemplate;
    private final StringProperty summarizationFullDocumentUserMessageTemplate;
    private final StringProperty citationParsingSystemMessageTemplate;
    private final StringProperty citationParsingUserMessageTemplate;
    private final StringProperty markdownChatExportTemplate;

    private final BooleanProperty generateFollowUpQuestions;
    private final IntegerProperty followUpQuestionsCount;
    private final StringProperty followUpQuestionsTemplate;

    private Runnable apiKeyChangeListener;

    public AiPreferences(
            boolean enableAi,
            boolean autoGenerateEmbeddings,
            boolean autoGenerateSummaries,
            AiProvider aiProvider,
            String openAiChatModel,
            String mistralAiChatModel,
            String geminiChatModel,
            String huggingFaceChatModel,
            boolean customizeExpertSettings,
            String openAiApiBaseUrl,
            String mistralAiApiBaseUrl,
            String geminiApiBaseUrl,
            String huggingFaceApiBaseUrl,
            SummarizatorKind summarizatorKind,
            TokenEstimatorKind tokenEstimatorKind,
            PredefinedEmbeddingModel embeddingModel,
            double temperature,
            int contextWindowSize,
            DocumentSplitterKind documentSplitterKind,
            int documentSplitterChunkSize,
            int documentSplitterOverlapSize,
            AnswerEngineKind answerEngineKind,
            int ragMaxResultsCount,
            double ragMinScore,
            String chattingSystemMessageTemplate,
            String chattingUserMessageTemplate,
            String summarizationChunkSystemMessageTemplate,
            String summarizationChunkUserMessageTemplate,
            String summarizationCombineSystemMessageTemplate,
            String summarizationCombineUserMessageTemplate,
            String summarizationFullDocumentSystemMessageTemplate,
            String summarizationFullDocumentUserMessageTemplate,
            String citationParsingSystemMessageTemplate,
            String citationParsingUserMessageTemplate,
            String markdownChatExportTemplate,
            boolean generateFollowUpQuestions,
            int followUpQuestionsCount,
            String followUpQuestionsTemplate
    ) {
        this.enableAi = new SimpleBooleanProperty(enableAi);
        this.autoGenerateEmbeddings = new SimpleBooleanProperty(autoGenerateEmbeddings);
        this.autoGenerateSummaries = new SimpleBooleanProperty(autoGenerateSummaries);

        this.aiProvider = new SimpleObjectProperty<>(aiProvider);

        this.openAiChatModel = new SimpleStringProperty(openAiChatModel);
        this.mistralAiChatModel = new SimpleStringProperty(mistralAiChatModel);
        this.geminiChatModel = new SimpleStringProperty(geminiChatModel);
        this.huggingFaceChatModel = new SimpleStringProperty(huggingFaceChatModel);

        this.customizeExpertSettings = new SimpleBooleanProperty(customizeExpertSettings);

        this.openAiApiBaseUrl = new SimpleStringProperty(openAiApiBaseUrl);
        this.mistralAiApiBaseUrl = new SimpleStringProperty(mistralAiApiBaseUrl);
        this.geminiApiBaseUrl = new SimpleStringProperty(geminiApiBaseUrl);
        this.huggingFaceApiBaseUrl = new SimpleStringProperty(huggingFaceApiBaseUrl);

        this.summarizatorKind = new SimpleObjectProperty<>(summarizatorKind);
        this.tokenEstimatorKind = new SimpleObjectProperty<>(tokenEstimatorKind);
        this.embeddingModel = new SimpleObjectProperty<>(embeddingModel);
        this.temperature = new SimpleDoubleProperty(temperature);
        this.contextWindowSize = new SimpleIntegerProperty(contextWindowSize);

        this.documentSplitterKind = new SimpleObjectProperty<>(documentSplitterKind);
        this.documentSplitterChunkSize = new SimpleIntegerProperty(documentSplitterChunkSize);
        this.documentSplitterOverlapSize = new SimpleIntegerProperty(documentSplitterOverlapSize);

        this.answerEngineKind = new SimpleObjectProperty<>(answerEngineKind);
        this.ragMaxResultsCount = new SimpleIntegerProperty(ragMaxResultsCount);
        this.ragMinScore = new SimpleDoubleProperty(ragMinScore);

        this.chattingSystemMessageTemplate = new SimpleStringProperty(chattingSystemMessageTemplate);
        this.chattingUserMessageTemplate = new SimpleStringProperty(chattingUserMessageTemplate);
        this.summarizationChunkSystemMessageTemplate = new SimpleStringProperty(summarizationChunkSystemMessageTemplate);
        this.summarizationChunkUserMessageTemplate = new SimpleStringProperty(summarizationChunkUserMessageTemplate);
        this.summarizationCombineSystemMessageTemplate = new SimpleStringProperty(summarizationCombineSystemMessageTemplate);
        this.summarizationCombineUserMessageTemplate = new SimpleStringProperty(summarizationCombineUserMessageTemplate);
        this.summarizationFullDocumentSystemMessageTemplate = new SimpleStringProperty(summarizationFullDocumentSystemMessageTemplate);
        this.summarizationFullDocumentUserMessageTemplate = new SimpleStringProperty(summarizationFullDocumentUserMessageTemplate);
        this.citationParsingSystemMessageTemplate = new SimpleStringProperty(citationParsingSystemMessageTemplate);
        this.citationParsingUserMessageTemplate = new SimpleStringProperty(citationParsingUserMessageTemplate);
        this.markdownChatExportTemplate = new SimpleStringProperty(markdownChatExportTemplate);

        this.generateFollowUpQuestions = new SimpleBooleanProperty(generateFollowUpQuestions);
        this.followUpQuestionsCount = new SimpleIntegerProperty(followUpQuestionsCount);
        this.followUpQuestionsTemplate = new SimpleStringProperty(followUpQuestionsTemplate);

        this.apiKeyChangeListener = () -> {
        };
    }

    public String getApiKeyForAiProvider(AiProvider aiProvider) {
        try (final Keyring keyring = Keyring.create()) {
            return keyring.getPassword(KEYRING_AI_SERVICE, KEYRING_AI_SERVICE_ACCOUNT + "-" + aiProvider.name());
        } catch (PasswordAccessException e) {
            LOGGER.debug("No API key stored for provider {}. Returning an empty string", aiProvider.getDisplayName());
            return "";
        } catch (Exception e) {
            LOGGER.warn("JabRef could not open keyring for retrieving {} API token", aiProvider.getDisplayName(), e);
            return "";
        }
    }

    public void storeAiApiKeyInKeyring(AiProvider aiProvider, String newKey) {
        try (final Keyring keyring = Keyring.create()) {
            if (StringUtil.isNullOrEmpty(newKey)) {
                try {
                    keyring.deletePassword(KEYRING_AI_SERVICE, KEYRING_AI_SERVICE_ACCOUNT + "-" + aiProvider.name());
                } catch (PasswordAccessException ex) {
                    LOGGER.debug("API key for provider {} not stored in keyring. JabRef does not store an empty key.", aiProvider.getDisplayName());
                }
            } else {
                keyring.setPassword(KEYRING_AI_SERVICE, KEYRING_AI_SERVICE_ACCOUNT + "-" + aiProvider.name(), newKey);
            }
        } catch (Exception e) {
            LOGGER.warn("JabRef could not open keyring for storing {} API token", aiProvider.getDisplayName(), e);
        }
    }

    public BooleanProperty enableAiProperty() {
        return enableAi;
    }

    public boolean getEnableAi() {
        return enableAi.get();
    }

    public void setEnableAi(boolean enableAi) {
        this.enableAi.set(enableAi);
    }

    public BooleanProperty autoGenerateEmbeddingsProperty() {
        return autoGenerateEmbeddings;
    }

    public boolean getAutoGenerateEmbeddings() {
        return autoGenerateEmbeddings.get();
    }

    public void setAutoGenerateEmbeddings(boolean autoGenerateEmbeddings) {
        this.autoGenerateEmbeddings.set(autoGenerateEmbeddings);
    }

    public BooleanProperty autoGenerateSummariesProperty() {
        return autoGenerateSummaries;
    }

    public boolean getAutoGenerateSummaries() {
        return autoGenerateSummaries.get();
    }

    public void setAutoGenerateSummaries(boolean autoGenerateSummaries) {
        this.autoGenerateSummaries.set(autoGenerateSummaries);
    }

    public ObjectProperty<AiProvider> aiProviderProperty() {
        return aiProvider;
    }

    public AiProvider getAiProvider() {
        return aiProvider.get();
    }

    public void setAiProvider(AiProvider aiProvider) {
        this.aiProvider.set(aiProvider);
    }

    public StringProperty openAiChatModelProperty() {
        return openAiChatModel;
    }

    public String getOpenAiChatModel() {
        return openAiChatModel.get();
    }

    public void setOpenAiChatModel(String openAiChatModel) {
        this.openAiChatModel.set(openAiChatModel);
    }

    public StringProperty mistralAiChatModelProperty() {
        return mistralAiChatModel;
    }

    public String getMistralAiChatModel() {
        return mistralAiChatModel.get();
    }

    public void setMistralAiChatModel(String mistralAiChatModel) {
        this.mistralAiChatModel.set(mistralAiChatModel);
    }

    public StringProperty geminiChatModelProperty() {
        return geminiChatModel;
    }

    public String getGeminiChatModel() {
        return geminiChatModel.get();
    }

    public void setGeminiChatModel(String geminiChatModel) {
        this.geminiChatModel.set(geminiChatModel);
    }

    public StringProperty huggingFaceChatModelProperty() {
        return huggingFaceChatModel;
    }

    public String getHuggingFaceChatModel() {
        return huggingFaceChatModel.get();
    }

    public void setHuggingFaceChatModel(String huggingFaceChatModel) {
        this.huggingFaceChatModel.set(huggingFaceChatModel);
    }

    public BooleanProperty customizeExpertSettingsProperty() {
        return customizeExpertSettings;
    }

    public boolean getCustomizeExpertSettings() {
        return customizeExpertSettings.get();
    }

    public void setCustomizeExpertSettings(boolean customizeExpertSettings) {
        this.customizeExpertSettings.set(customizeExpertSettings);
    }

    public ObjectProperty<SummarizatorKind> summarizatorKindProperty() {
        return summarizatorKind;
    }

    public SummarizatorKind getSummarizatorKind() {
        return summarizatorKind.get();
    }

    public void setSummarizatorKind(SummarizatorKind summarizatorKind) {
        this.summarizatorKind.set(summarizatorKind);
    }

    public ObjectProperty<TokenEstimatorKind> tokenEstimatorKindProperty() {
        return tokenEstimatorKind;
    }

    public TokenEstimatorKind getTokenEstimatorKind() {
        return tokenEstimatorKind.get();
    }

    public void setTokenEstimatorKind(TokenEstimatorKind tokenEstimatorKind) {
        this.tokenEstimatorKind.set(tokenEstimatorKind);
    }

    public ObjectProperty<PredefinedEmbeddingModel> embeddingModelProperty() {
        return embeddingModel;
    }

    public PredefinedEmbeddingModel getEmbeddingModel() {
        if (getCustomizeExpertSettings()) {
            return embeddingModel.get();
        } else {
            return AiDefaultExpertSettings.EMBEDDING_MODEL;
        }
    }

    public void setEmbeddingModel(PredefinedEmbeddingModel embeddingModel) {
        this.embeddingModel.set(embeddingModel);
    }

    public StringProperty openAiApiBaseUrlProperty() {
        return openAiApiBaseUrl;
    }

    public String getOpenAiApiBaseUrl() {
        return openAiApiBaseUrl.get();
    }

    public void setOpenAiApiBaseUrl(String openAiApiBaseUrl) {
        this.openAiApiBaseUrl.set(openAiApiBaseUrl);
    }

    public StringProperty mistralAiApiBaseUrlProperty() {
        return mistralAiApiBaseUrl;
    }

    public String getMistralAiApiBaseUrl() {
        return mistralAiApiBaseUrl.get();
    }

    public void setMistralAiApiBaseUrl(String mistralAiApiBaseUrl) {
        this.mistralAiApiBaseUrl.set(mistralAiApiBaseUrl);
    }

    public StringProperty geminiApiBaseUrlProperty() {
        return geminiApiBaseUrl;
    }

    public String getGeminiApiBaseUrl() {
        return geminiApiBaseUrl.get();
    }

    public void setGeminiApiBaseUrl(String geminiApiBaseUrl) {
        this.geminiApiBaseUrl.set(geminiApiBaseUrl);
    }

    public StringProperty huggingFaceApiBaseUrlProperty() {
        return huggingFaceApiBaseUrl;
    }

    public String getHuggingFaceApiBaseUrl() {
        return huggingFaceApiBaseUrl.get();
    }

    public void setHuggingFaceApiBaseUrl(String huggingFaceApiBaseUrl) {
        this.huggingFaceApiBaseUrl.set(huggingFaceApiBaseUrl);
    }

    public DoubleProperty temperatureProperty() {
        return temperature;
    }

    public double getTemperature() {
        if (getCustomizeExpertSettings()) {
            return temperature.get();
        } else {
            return AiDefaultExpertSettings.TEMPERATURE;
        }
    }

    public void setTemperature(double temperature) {
        this.temperature.set(temperature);
    }

    public IntegerProperty contextWindowSizeProperty() {
        return contextWindowSize;
    }

    public int getContextWindowSize() {
        if (getCustomizeExpertSettings()) {
            return contextWindowSize.get();
        } else {
            return switch (aiProvider.get()) {
                case OPEN_AI ->
                        PredefinedChatModel.getContextWindowSize(AiProvider.OPEN_AI, openAiChatModel.get());
                case MISTRAL_AI ->
                        PredefinedChatModel.getContextWindowSize(AiProvider.MISTRAL_AI, mistralAiChatModel.get());
                case HUGGING_FACE ->
                        PredefinedChatModel.getContextWindowSize(AiProvider.HUGGING_FACE, huggingFaceChatModel.get());
                case GEMINI ->
                        PredefinedChatModel.getContextWindowSize(AiProvider.GEMINI, geminiChatModel.get());
            };
        }
    }

    public void setContextWindowSize(int contextWindowSize) {
        this.contextWindowSize.set(contextWindowSize);
    }

    public ObjectProperty<DocumentSplitterKind> documentSplitterKindProperty() {
        return documentSplitterKind;
    }

    public DocumentSplitterKind getDocumentSplitterKind() {
        return documentSplitterKind.get();
    }

    public void setDocumentSplitterKind(DocumentSplitterKind documentSplitterKind) {
        this.documentSplitterKind.set(documentSplitterKind);
    }

    public IntegerProperty documentSplitterChunkSizeProperty() {
        return documentSplitterChunkSize;
    }

    public int getDocumentSplitterChunkSize() {
        if (getCustomizeExpertSettings()) {
            return documentSplitterChunkSize.get();
        } else {
            return AiDefaultExpertSettings.DOCUMENT_SPLITTER_CHUNK_SIZE;
        }
    }

    public void setDocumentSplitterChunkSize(int documentSplitterChunkSize) {
        this.documentSplitterChunkSize.set(documentSplitterChunkSize);
    }

    public IntegerProperty documentSplitterOverlapSizeProperty() {
        return documentSplitterOverlapSize;
    }

    public int getDocumentSplitterOverlapSize() {
        if (getCustomizeExpertSettings()) {
            return documentSplitterOverlapSize.get();
        } else {
            return AiDefaultExpertSettings.DOCUMENT_SPLITTER_OVERLAP_SIZE;
        }
    }

    public void setDocumentSplitterOverlapSize(int documentSplitterOverlapSize) {
        this.documentSplitterOverlapSize.set(documentSplitterOverlapSize);
    }

    public ObjectProperty<AnswerEngineKind> answerEngineKindProperty() {
        return answerEngineKind;
    }

    public AnswerEngineKind getAnswerEngineKind() {
        return answerEngineKind.get();
    }

    public void setAnswerEngineKind(AnswerEngineKind answerEngineKind) {
        this.answerEngineKind.set(answerEngineKind);
    }

    public IntegerProperty ragMaxResultsCountProperty() {
        return ragMaxResultsCount;
    }

    public int getRagMaxResultsCount() {
        if (getCustomizeExpertSettings()) {
            return ragMaxResultsCount.get();
        } else {
            return AiDefaultExpertSettings.RAG_MAX_RESULTS_COUNT;
        }
    }

    public void setRagMaxResultsCount(int ragMaxResultsCount) {
        this.ragMaxResultsCount.set(ragMaxResultsCount);
    }

    public DoubleProperty ragMinScoreProperty() {
        return ragMinScore;
    }

    public double getRagMinScore() {
        if (getCustomizeExpertSettings()) {
            return ragMinScore.get();
        } else {
            return AiDefaultExpertSettings.RAG_MIN_SCORE;
        }
    }

    public void setRagMinScore(double ragMinScore) {
        this.ragMinScore.set(ragMinScore);
    }

    /**
     * Listen to changes of preferences that are related to embeddings generation.
     *
     * @param runnable The runnable that should be executed when the preferences change.
     */
    public void addListenerToEmbeddingsParametersChange(Runnable runnable) {
        embeddingModel.addListener((observableValue, oldValue, newValue) -> {
            if (newValue != oldValue) {
                runnable.run();
            }
        });

        documentSplitterChunkSize.addListener((observableValue, oldValue, newValue) -> {
            if (!Objects.equals(newValue, oldValue)) {
                runnable.run();
            }
        });

        documentSplitterOverlapSize.addListener((observableValue, oldValue, newValue) -> {
            if (!Objects.equals(newValue, oldValue)) {
                runnable.run();
            }
        });
    }

    public void addListenerToChatModels(Runnable runnable) {
        List<Property<?>> observables = List.of(openAiChatModel, mistralAiChatModel, huggingFaceChatModel, geminiChatModel);

        observables.forEach(obs -> obs.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        }));
    }

    public void addListenerToApiBaseUrls(Runnable runnable) {
        List<Property<?>> observables = List.of(openAiApiBaseUrl, mistralAiApiBaseUrl, huggingFaceApiBaseUrl, geminiApiBaseUrl);

        observables.forEach(obs -> obs.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        }));
    }

    public String getSelectedChatModel() {
        return switch (aiProvider.get()) {
            case OPEN_AI ->
                    openAiChatModel.get();
            case MISTRAL_AI ->
                    mistralAiChatModel.get();
            case HUGGING_FACE ->
                    huggingFaceChatModel.get();
            case GEMINI ->
                    geminiChatModel.get();
        };
    }

    public String getSelectedApiBaseUrl() {
        if (customizeExpertSettings.get()) {
            return switch (aiProvider.get()) {
                case OPEN_AI ->
                        openAiApiBaseUrl.get();
                case MISTRAL_AI ->
                        mistralAiApiBaseUrl.get();
                case HUGGING_FACE ->
                        huggingFaceApiBaseUrl.get();
                case GEMINI ->
                        geminiApiBaseUrl.get();
            };
        } else {
            return aiProvider.get().getApiUrl();
        }
    }

    public void setApiKeyChangeListener(Runnable apiKeyChangeListener) {
        this.apiKeyChangeListener = apiKeyChangeListener;
    }

    /**
     * Notify that the API key has been updated.
     */
    public void apiKeyUpdated() {
        apiKeyChangeListener.run();
    }

    public StringProperty chattingSystemMessageTemplateProperty() {
        return chattingSystemMessageTemplate;
    }

    public String getChattingSystemMessageTemplate() {
        return chattingSystemMessageTemplate.get();
    }

    public void setChattingSystemMessageTemplate(String template) {
        chattingSystemMessageTemplate.set(template);
    }

    public StringProperty chattingUserMessageTemplateProperty() {
        return chattingUserMessageTemplate;
    }

    public String getChattingUserMessageTemplate() {
        return chattingUserMessageTemplate.get();
    }

    public void setChattingUserMessageTemplate(String template) {
        chattingUserMessageTemplate.set(template);
    }

    public StringProperty summarizationChunkSystemMessageTemplateProperty() {
        return summarizationChunkSystemMessageTemplate;
    }

    public String getSummarizationChunkSystemMessageTemplate() {
        return summarizationChunkSystemMessageTemplate.get();
    }

    public void setSummarizationChunkSystemMessageTemplate(String template) {
        summarizationChunkSystemMessageTemplate.set(template);
    }

    public StringProperty summarizationChunkUserMessageTemplateProperty() {
        return summarizationChunkUserMessageTemplate;
    }

    public String getSummarizationChunkUserMessageTemplate() {
        return summarizationChunkUserMessageTemplate.get();
    }

    public void setSummarizationChunkUserMessageTemplate(String template) {
        summarizationChunkUserMessageTemplate.set(template);
    }

    public StringProperty summarizationCombineSystemMessageTemplateProperty() {
        return summarizationCombineSystemMessageTemplate;
    }

    public String getSummarizationCombineSystemMessageTemplate() {
        return summarizationCombineSystemMessageTemplate.get();
    }

    public void setSummarizationCombineSystemMessageTemplate(String template) {
        summarizationCombineSystemMessageTemplate.set(template);
    }

    public StringProperty summarizationCombineUserMessageTemplateProperty() {
        return summarizationCombineUserMessageTemplate;
    }

    public String getSummarizationCombineUserMessageTemplate() {
        return summarizationCombineUserMessageTemplate.get();
    }

    public void setSummarizationCombineUserMessageTemplate(String template) {
        summarizationCombineUserMessageTemplate.set(template);
    }

    public StringProperty summarizationFullDocumentSystemMessageTemplateProperty() {
        return summarizationFullDocumentSystemMessageTemplate;
    }

    public String getSummarizationFullDocumentSystemMessageTemplate() {
        return summarizationFullDocumentSystemMessageTemplate.get();
    }

    public void setSummarizationFullDocumentSystemMessageTemplate(String template) {
        summarizationFullDocumentSystemMessageTemplate.set(template);
    }

    public StringProperty summarizationFullDocumentUserMessageTemplateProperty() {
        return summarizationFullDocumentUserMessageTemplate;
    }

    public String getSummarizationFullDocumentUserMessageTemplate() {
        return summarizationFullDocumentUserMessageTemplate.get();
    }

    public void setSummarizationFullDocumentUserMessageTemplate(String template) {
        summarizationFullDocumentUserMessageTemplate.set(template);
    }

    public StringProperty citationParsingSystemMessageTemplateProperty() {
        return citationParsingSystemMessageTemplate;
    }

    public String getCitationParsingSystemMessageTemplate() {
        return citationParsingSystemMessageTemplate.get();
    }

    public void setCitationParsingSystemMessageTemplate(String template) {
        citationParsingSystemMessageTemplate.set(template);
    }

    public StringProperty citationParsingUserMessageTemplateProperty() {
        return citationParsingUserMessageTemplate;
    }

    public String getCitationParsingUserMessageTemplate() {
        return citationParsingUserMessageTemplate.get();
    }

    public void setCitationParsingUserMessageTemplate(String template) {
        citationParsingUserMessageTemplate.set(template);
    }

    public StringProperty markdownChatExportTemplateProperty() {
        return markdownChatExportTemplate;
    }

    public String getMarkdownChatExportTemplate() {
        return markdownChatExportTemplate.get();
    }

    public void setMarkdownChatExportTemplate(String template) {
        markdownChatExportTemplate.set(template);
    }

    public BooleanProperty generateFollowUpQuestionsProperty() {
        return generateFollowUpQuestions;
    }

    public boolean getGenerateFollowUpQuestions() {
        return generateFollowUpQuestions.get();
    }

    public void setGenerateFollowUpQuestions(boolean generateFollowUpQuestions) {
        this.generateFollowUpQuestions.set(generateFollowUpQuestions);
    }

    public IntegerProperty followUpQuestionsCountProperty() {
        return followUpQuestionsCount;
    }

    public int getFollowUpQuestionsCount() {
        return followUpQuestionsCount.get();
    }

    public void setFollowUpQuestionsCount(int followUpQuestionsCount) {
        this.followUpQuestionsCount.set(followUpQuestionsCount);
    }

    public StringProperty followUpQuestionsTemplateProperty() {
        return followUpQuestionsTemplate;
    }

    public String getFollowUpQuestionsTemplate() {
        return followUpQuestionsTemplate.get();
    }

    public void setFollowUpQuestionsTemplate(String followUpQuestionsTemplate) {
        this.followUpQuestionsTemplate.set(followUpQuestionsTemplate);
    }
}
