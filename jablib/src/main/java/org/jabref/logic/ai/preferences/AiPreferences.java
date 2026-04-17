package org.jabref.logic.ai.preferences;

import java.util.List;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jabref.model.ai.embeddings.EmbeddingModelEnumeration;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

public class AiPreferences {
    private final BooleanProperty enableAi;
    private final BooleanProperty autoGenerateEmbeddings;
    private final BooleanProperty autoGenerateSummaries;

    private final IntegerProperty selectedProfileIndex;
    private final ObservableList<String> profileNames;
    private final ObservableList<String> profileProviders;
    private final ObservableList<String> profileChatModels;
    private final ObservableList<String> profileApiBaseUrls;
    private final ObservableList<String> profileApiKeys;
    private final ObservableList<String> profileTemperatures;
    private final ObservableList<Integer> profileContextWindowSizes;
    private final ObservableList<String> profileTokenEstimators;

    private final ObjectProperty<SummarizatorKind> summarizatorKind;
    private final ObjectProperty<EmbeddingModelEnumeration> embeddingModel;

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

    public AiPreferences(
            boolean enableAi,
            boolean autoGenerateEmbeddings,
            boolean autoGenerateSummaries,
            int selectedProfileIndex,
            List<String> profileNames,
            List<String> profileProviders,
            List<String> profileChatModels,
            List<String> profileApiBaseUrls,
            List<String> profileApiKeys,
            List<String> profileTemperatures,
            List<Integer> profileContextWindowSizes,
            List<String> profileTokenEstimators,
            SummarizatorKind summarizatorKind,
            EmbeddingModelEnumeration embeddingModel,
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

        this.selectedProfileIndex = new SimpleIntegerProperty(selectedProfileIndex);
        this.profileNames = FXCollections.observableArrayList(profileNames);
        this.profileProviders = FXCollections.observableArrayList(profileProviders);
        this.profileChatModels = FXCollections.observableArrayList(profileChatModels);
        this.profileApiBaseUrls = FXCollections.observableArrayList(profileApiBaseUrls);
        this.profileApiKeys = FXCollections.observableArrayList(profileApiKeys);
        this.profileTemperatures = FXCollections.observableArrayList(profileTemperatures);
        this.profileContextWindowSizes = FXCollections.observableArrayList(profileContextWindowSizes);
        this.profileTokenEstimators = FXCollections.observableArrayList(profileTokenEstimators);

        this.summarizatorKind = new SimpleObjectProperty<>(summarizatorKind);
        this.embeddingModel = new SimpleObjectProperty<>(embeddingModel);

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
    }

    public IntegerProperty selectedProfileIndexProperty() {
        return selectedProfileIndex;
    }

    public int getSelectedProfileIndex() {
        return selectedProfileIndex.get();
    }

    public void setSelectedProfileIndex(int index) {
        selectedProfileIndex.set(index);
    }

    public ObservableList<String> getProfileNames() {
        return profileNames;
    }

    public ObservableList<String> getProfileProviders() {
        return profileProviders;
    }

    public ObservableList<String> getProfileChatModels() {
        return profileChatModels;
    }

    public ObservableList<String> getProfileApiBaseUrls() {
        return profileApiBaseUrls;
    }

    public ObservableList<String> getProfileApiKeys() {
        return profileApiKeys;
    }

    public ObservableList<String> getProfileTemperatures() {
        return profileTemperatures;
    }

    public ObservableList<Integer> getProfileContextWindowSizes() {
        return profileContextWindowSizes;
    }

    public ObservableList<String> getProfileTokenEstimators() {
        return profileTokenEstimators;
    }

    // -------------------------------------------------------------------------
    // Enable AI / Auto-generate
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Expert settings
    // -------------------------------------------------------------------------

    public ObjectProperty<SummarizatorKind> summarizatorKindProperty() {
        return summarizatorKind;
    }

    public SummarizatorKind getSummarizatorKind() {
        return summarizatorKind.get();
    }

    public void setSummarizatorKind(SummarizatorKind summarizatorKind) {
        this.summarizatorKind.set(summarizatorKind);
    }

    public ObjectProperty<EmbeddingModelEnumeration> embeddingModelProperty() {
        return embeddingModel;
    }

    public EmbeddingModelEnumeration getEmbeddingModel() {
        return embeddingModel.get();
    }

    public void setEmbeddingModel(EmbeddingModelEnumeration embeddingModel) {
        this.embeddingModel.set(embeddingModel);
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
        return documentSplitterChunkSize.get();
    }

    public void setDocumentSplitterChunkSize(int documentSplitterChunkSize) {
        this.documentSplitterChunkSize.set(documentSplitterChunkSize);
    }

    public IntegerProperty documentSplitterOverlapSizeProperty() {
        return documentSplitterOverlapSize;
    }

    public int getDocumentSplitterOverlapSize() {
        return documentSplitterOverlapSize.get();
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
        return ragMaxResultsCount.get();
    }

    public void setRagMaxResultsCount(int ragMaxResultsCount) {
        this.ragMaxResultsCount.set(ragMaxResultsCount);
    }

    public DoubleProperty ragMinScoreProperty() {
        return ragMinScore;
    }

    public double getRagMinScore() {
        return ragMinScore.get();
    }

    public void setRagMinScore(double ragMinScore) {
        this.ragMinScore.set(ragMinScore);
    }

    // -------------------------------------------------------------------------
    // Listeners for chat model / API base-URL changes (used by background workers)
    // -------------------------------------------------------------------------

    /**
     * Listen to changes of preferences that are related to embeddings generation.
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
        profileChatModels.addListener((ListChangeListener<String>) _ -> runnable.run());
        selectedProfileIndex.addListener((_, _, _) -> runnable.run());
    }

    public void addListenerToApiBaseUrls(Runnable runnable) {
        profileApiBaseUrls.addListener((ListChangeListener<String>) _ -> runnable.run());
        selectedProfileIndex.addListener((_, _, _) -> runnable.run());
    }

    public void addListenerToApiKeys(Runnable runnable) {
        profileApiKeys.addListener((ListChangeListener<String>) _ -> runnable.run());
        selectedProfileIndex.addListener((_, _, _) -> runnable.run());
    }

    // -------------------------------------------------------------------------
    // Templates
    // -------------------------------------------------------------------------

    public StringProperty chattingSystemMessageTemplateProperty() {
        return chattingSystemMessageTemplate;
    }

    public String getChattingSystemMessageTemplate() {
        return chattingSystemMessageTemplate.get();
    }

    public void setChattingSystemMessageTemplate(String chattingSystemMessageTemplate) {
        this.chattingSystemMessageTemplate.set(chattingSystemMessageTemplate);
    }

    public StringProperty chattingUserMessageTemplateProperty() {
        return chattingUserMessageTemplate;
    }

    public String getChattingUserMessageTemplate() {
        return chattingUserMessageTemplate.get();
    }

    public void setChattingUserMessageTemplate(String chattingUserMessageTemplate) {
        this.chattingUserMessageTemplate.set(chattingUserMessageTemplate);
    }

    public StringProperty summarizationChunkSystemMessageTemplateProperty() {
        return summarizationChunkSystemMessageTemplate;
    }

    public String getSummarizationChunkSystemMessageTemplate() {
        return summarizationChunkSystemMessageTemplate.get();
    }

    public void setSummarizationChunkSystemMessageTemplate(String summarizationChunkSystemMessageTemplate) {
        this.summarizationChunkSystemMessageTemplate.set(summarizationChunkSystemMessageTemplate);
    }

    public StringProperty summarizationChunkUserMessageTemplateProperty() {
        return summarizationChunkUserMessageTemplate;
    }

    public String getSummarizationChunkUserMessageTemplate() {
        return summarizationChunkUserMessageTemplate.get();
    }

    public void setSummarizationChunkUserMessageTemplate(String summarizationChunkUserMessageTemplate) {
        this.summarizationChunkUserMessageTemplate.set(summarizationChunkUserMessageTemplate);
    }

    public StringProperty summarizationCombineSystemMessageTemplateProperty() {
        return summarizationCombineSystemMessageTemplate;
    }

    public String getSummarizationCombineSystemMessageTemplate() {
        return summarizationCombineSystemMessageTemplate.get();
    }

    public void setSummarizationCombineSystemMessageTemplate(String summarizationCombineSystemMessageTemplate) {
        this.summarizationCombineSystemMessageTemplate.set(summarizationCombineSystemMessageTemplate);
    }

    public StringProperty summarizationCombineUserMessageTemplateProperty() {
        return summarizationCombineUserMessageTemplate;
    }

    public String getSummarizationCombineUserMessageTemplate() {
        return summarizationCombineUserMessageTemplate.get();
    }

    public void setSummarizationCombineUserMessageTemplate(String summarizationCombineUserMessageTemplate) {
        this.summarizationCombineUserMessageTemplate.set(summarizationCombineUserMessageTemplate);
    }

    public StringProperty summarizationFullDocumentSystemMessageTemplateProperty() {
        return summarizationFullDocumentSystemMessageTemplate;
    }

    public String getSummarizationFullDocumentSystemMessageTemplate() {
        return summarizationFullDocumentSystemMessageTemplate.get();
    }

    public void setSummarizationFullDocumentSystemMessageTemplate(String template) {
        this.summarizationFullDocumentSystemMessageTemplate.set(template);
    }

    public StringProperty summarizationFullDocumentUserMessageTemplateProperty() {
        return summarizationFullDocumentUserMessageTemplate;
    }

    public String getSummarizationFullDocumentUserMessageTemplate() {
        return summarizationFullDocumentUserMessageTemplate.get();
    }

    public void setSummarizationFullDocumentUserMessageTemplate(String template) {
        this.summarizationFullDocumentUserMessageTemplate.set(template);
    }

    public StringProperty citationParsingSystemMessageTemplateProperty() {
        return citationParsingSystemMessageTemplate;
    }

    public String getCitationParsingSystemMessageTemplate() {
        return citationParsingSystemMessageTemplate.get();
    }

    public void setCitationParsingSystemMessageTemplate(String citationParsingSystemMessageTemplate) {
        this.citationParsingSystemMessageTemplate.set(citationParsingSystemMessageTemplate);
    }

    public StringProperty citationParsingUserMessageTemplateProperty() {
        return citationParsingUserMessageTemplate;
    }

    public String getCitationParsingUserMessageTemplate() {
        return citationParsingUserMessageTemplate.get();
    }

    public void setCitationParsingUserMessageTemplate(String citationParsingUserMessageTemplate) {
        this.citationParsingUserMessageTemplate.set(citationParsingUserMessageTemplate);
    }

    public StringProperty markdownChatExportTemplateProperty() {
        return markdownChatExportTemplate;
    }

    public String getMarkdownChatExportTemplate() {
        return markdownChatExportTemplate.get();
    }

    public void setMarkdownChatExportTemplate(String markdownChatExportTemplate) {
        this.markdownChatExportTemplate.set(markdownChatExportTemplate);
    }

    // -------------------------------------------------------------------------
    // Follow-up questions
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Convenience methods for selected profile
    // -------------------------------------------------------------------------

    public ObjectProperty<AiProvider> aiProviderProperty() {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileProviders.size()) {
            try {
                return new SimpleObjectProperty<>(AiProvider.valueOf(profileProviders.get(index)));
            } catch (IllegalArgumentException e) {
                return new SimpleObjectProperty<>(AiProvider.OPEN_AI);
            }
        }
        return new SimpleObjectProperty<>(AiProvider.OPEN_AI);
    }

    public AiProvider getAiProvider() {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileProviders.size()) {
            try {
                return AiProvider.valueOf(profileProviders.get(index));
            } catch (IllegalArgumentException e) {
                return AiProvider.OPEN_AI;
            }
        }
        return AiProvider.OPEN_AI;
    }

    public String getSelectedChatModel() {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileChatModels.size()) {
            return profileChatModels.get(index);
        }
        return "";
    }

    public String getSelectedApiBaseUrl() {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileApiBaseUrls.size()) {
            return profileApiBaseUrls.get(index);
        }
        return "";
    }

    public String getSelectedApiKey() {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileApiKeys.size()) {
            return profileApiKeys.get(index);
        }
        return "";
    }

    public DoubleProperty temperatureProperty() {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileTemperatures.size()) {
            try {
                return new SimpleDoubleProperty(Double.parseDouble(profileTemperatures.get(index)));
            } catch (NumberFormatException e) {
                return new SimpleDoubleProperty(0.0);
            }
        }
        return new SimpleDoubleProperty(0.0);
    }

    public double getTemperature() {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileTemperatures.size()) {
            try {
                return Double.parseDouble(profileTemperatures.get(index));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    public int getContextWindowSize() {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileContextWindowSizes.size()) {
            return profileContextWindowSizes.get(index);
        }
        return 0;
    }

    public TokenEstimatorKind getTokenEstimatorKind() {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileTokenEstimators.size()) {
            try {
                return TokenEstimatorKind.valueOf(profileTokenEstimators.get(index));
            } catch (IllegalArgumentException e) {
                return TokenEstimatorKind.AVERAGE;
            }
        }
        return TokenEstimatorKind.AVERAGE;
    }

    public String getApiKeyForProfile(int index) {
        if (index >= 0 && index < profileApiKeys.size()) {
            return profileApiKeys.get(index);
        }
        return "";
    }

    public void storeApiKeyForProfile(int index, String apiKey) {
        if (index >= 0 && index < profileApiKeys.size()) {
            profileApiKeys.set(index, apiKey);
        }
    }

    public void setApiKeyChangeListener(Runnable listener) {
        addListenerToApiKeys(listener);
    }

    public void setAiProvider(AiProvider provider) {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileProviders.size()) {
            profileProviders.set(index, provider.name());
        }
    }

    public void setTemperature(double temperature) {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileTemperatures.size()) {
            profileTemperatures.set(index, String.valueOf(temperature));
        }
    }

    public void setContextWindowSize(int size) {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileContextWindowSizes.size()) {
            profileContextWindowSizes.set(index, size);
        }
    }

    public void setTokenEstimatorKind(TokenEstimatorKind kind) {
        int index = getSelectedProfileIndex();
        if (index >= 0 && index < profileTokenEstimators.size()) {
            profileTokenEstimators.set(index, kind.name());
        }
    }

    // These methods are deprecated placeholders for backwards compatibility
    // The new profile-based system doesn't have per-provider chat models at the preferences level
    // Instead, users should use profiles
    @Deprecated
    public String getOpenAiChatModel() {
        // Return the chat model of the first profile with OPEN_AI provider
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("OPEN_AI".equals(profileProviders.get(i)) && i < profileChatModels.size()) {
                return profileChatModels.get(i);
            }
        }
        return "";
    }

    @Deprecated
    public String getMistralAiChatModel() {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("MISTRAL_AI".equals(profileProviders.get(i)) && i < profileChatModels.size()) {
                return profileChatModels.get(i);
            }
        }
        return "";
    }

    @Deprecated
    public String getGeminiChatModel() {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("GEMINI".equals(profileProviders.get(i)) && i < profileChatModels.size()) {
                return profileChatModels.get(i);
            }
        }
        return "";
    }

    @Deprecated
    public String getHuggingFaceChatModel() {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("HUGGING_FACE".equals(profileProviders.get(i)) && i < profileChatModels.size()) {
                return profileChatModels.get(i);
            }
        }
        return "";
    }

    @Deprecated
    public void setOpenAiChatModel(String model) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("OPEN_AI".equals(profileProviders.get(i)) && i < profileChatModels.size()) {
                profileChatModels.set(i, model);
                return;
            }
        }
    }

    @Deprecated
    public void setMistralAiChatModel(String model) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("MISTRAL_AI".equals(profileProviders.get(i)) && i < profileChatModels.size()) {
                profileChatModels.set(i, model);
                return;
            }
        }
    }

    @Deprecated
    public void setGeminiChatModel(String model) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("GEMINI".equals(profileProviders.get(i)) && i < profileChatModels.size()) {
                profileChatModels.set(i, model);
                return;
            }
        }
    }

    @Deprecated
    public void setHuggingFaceChatModel(String model) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("HUGGING_FACE".equals(profileProviders.get(i)) && i < profileChatModels.size()) {
                profileChatModels.set(i, model);
                return;
            }
        }
    }

    @Deprecated
    public String getApiKeyForAiProvider(AiProvider provider) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if (provider.name().equals(profileProviders.get(i)) && i < profileApiKeys.size()) {
                return profileApiKeys.get(i);
            }
        }
        return "";
    }

    @Deprecated
    public void storeAiApiKeyInKeyring(AiProvider provider, String apiKey) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if (provider.name().equals(profileProviders.get(i)) && i < profileApiKeys.size()) {
                profileApiKeys.set(i, apiKey);
                return;
            }
        }
    }

    @Deprecated
    public void apiKeyUpdated() {
        // Placeholder for compatibility - in the old system this triggered listeners
        // In the new profile system, listeners are already attached to profileApiKeys
    }

    @Deprecated
    public boolean getCustomizeExpertSettings() {
        // This was a UI-only setting - return true by default
        return true;
    }

    @Deprecated
    public void setCustomizeExpertSettings(boolean customize) {
        // Placeholder for compatibility - this was a UI-only setting
    }

    @Deprecated
    public String getOpenAiApiBaseUrl() {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("OPEN_AI".equals(profileProviders.get(i)) && i < profileApiBaseUrls.size()) {
                return profileApiBaseUrls.get(i);
            }
        }
        return AiProvider.OPEN_AI.getApiUrl();
    }

    @Deprecated
    public String getMistralAiApiBaseUrl() {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("MISTRAL_AI".equals(profileProviders.get(i)) && i < profileApiBaseUrls.size()) {
                return profileApiBaseUrls.get(i);
            }
        }
        return AiProvider.MISTRAL_AI.getApiUrl();
    }

    @Deprecated
    public String getGeminiApiBaseUrl() {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("GEMINI".equals(profileProviders.get(i)) && i < profileApiBaseUrls.size()) {
                return profileApiBaseUrls.get(i);
            }
        }
        return AiProvider.GEMINI.getApiUrl();
    }

    @Deprecated
    public String getHuggingFaceApiBaseUrl() {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("HUGGING_FACE".equals(profileProviders.get(i)) && i < profileApiBaseUrls.size()) {
                return profileApiBaseUrls.get(i);
            }
        }
        return AiProvider.HUGGING_FACE.getApiUrl();
    }

    @Deprecated
    public void setOpenAiApiBaseUrl(String url) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("OPEN_AI".equals(profileProviders.get(i)) && i < profileApiBaseUrls.size()) {
                profileApiBaseUrls.set(i, url);
                return;
            }
        }
    }

    @Deprecated
    public void setMistralAiApiBaseUrl(String url) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("MISTRAL_AI".equals(profileProviders.get(i)) && i < profileApiBaseUrls.size()) {
                profileApiBaseUrls.set(i, url);
                return;
            }
        }
    }

    @Deprecated
    public void setGeminiApiBaseUrl(String url) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("GEMINI".equals(profileProviders.get(i)) && i < profileApiBaseUrls.size()) {
                profileApiBaseUrls.set(i, url);
                return;
            }
        }
    }

    @Deprecated
    public void setHuggingFaceApiBaseUrl(String url) {
        for (int i = 0; i < profileProviders.size(); i++) {
            if ("HUGGING_FACE".equals(profileProviders.get(i)) && i < profileApiBaseUrls.size()) {
                profileApiBaseUrls.set(i, url);
                return;
            }
        }
    }
}
