package org.jabref.preferences.ai;

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

import org.jabref.logic.ai.AiDefaultPreferences;
import org.jabref.model.strings.StringUtil;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiProfile {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiProfile.class);

    private static final String KEYRING_AI_SERVICE = "org.jabref.ai";
    private static final String KEYRING_AI_SERVICE_ACCOUNT = "apiKey";

    private final StringProperty name;

    private final ObjectProperty<AiProvider> aiProvider;
    private final StringProperty chatModel;

    private final BooleanProperty customizeExpertSettings;

    private final StringProperty apiBaseUrl;
    private final ObjectProperty<EmbeddingModel> embeddingModel;
    private final StringProperty instruction;
    private final DoubleProperty temperature;
    private final IntegerProperty contextWindowSize;
    private final IntegerProperty documentSplitterChunkSize;
    private final IntegerProperty documentSplitterOverlapSize;
    private final IntegerProperty ragMaxResultsCount;
    private final DoubleProperty ragMinScore;

    public AiProfile(String name,
                     AiProvider aiProvider,
                     String chatModel,
                     boolean customizeExpertSettings,
                     String apiBaseUrl,
                     EmbeddingModel embeddingModel,
                     String instruction,
                     double temperature,
                     int contextWindowSize,
                     int documentSplitterChunkSize,
                     int documentSplitterOverlapSize,
                     int ragMaxResultsCount,
                     double ragMinScore
    ) {
        this.name = new SimpleStringProperty(name);

        this.aiProvider = new SimpleObjectProperty<>(aiProvider);
        this.chatModel = new SimpleStringProperty(chatModel);

        this.customizeExpertSettings = new SimpleBooleanProperty(customizeExpertSettings);

        this.apiBaseUrl = new SimpleStringProperty(apiBaseUrl);
        this.embeddingModel = new SimpleObjectProperty<>(embeddingModel);
        this.instruction = new SimpleStringProperty(instruction);
        this.temperature = new SimpleDoubleProperty(temperature);
        this.contextWindowSize = new SimpleIntegerProperty(contextWindowSize);
        this.documentSplitterChunkSize = new SimpleIntegerProperty(documentSplitterChunkSize);
        this.documentSplitterOverlapSize = new SimpleIntegerProperty(documentSplitterOverlapSize);
        this.ragMaxResultsCount = new SimpleIntegerProperty(ragMaxResultsCount);
        this.ragMinScore = new SimpleDoubleProperty(ragMinScore);
    }

    public String loadApiKey() {
        try (final Keyring keyring = Keyring.create()) {
            return keyring.getPassword(KEYRING_AI_SERVICE, KEYRING_AI_SERVICE_ACCOUNT + "-" + name.get());
        } catch (
                PasswordAccessException e) {
            LOGGER.debug("No API key stored for AI profile '{}'. Returning an empty string", name.get());
            return "";
        } catch (Exception e) {
            LOGGER.warn("JabRef could not open keyring for retrieving API token of AI profile '{}'", name.get(), e);
            return "";
        }
    }

    public void storeApiKey(String newKey) {
        try (final Keyring keyring = Keyring.create()) {
            if (StringUtil.isNullOrEmpty(newKey)) {
                try {
                    keyring.deletePassword(KEYRING_AI_SERVICE, KEYRING_AI_SERVICE_ACCOUNT + "-" + name.get());
                } catch (PasswordAccessException ex) {
                    LOGGER.debug("API key for AI profile '{}' not stored in keyring. JabRef does not store an empty key.", name.get());
                }
            } else {
                keyring.setPassword(KEYRING_AI_SERVICE, KEYRING_AI_SERVICE_ACCOUNT + "-" + name.get(), newKey);
            }
        } catch (Exception e) {
            LOGGER.warn("JabRef could not open keyring for storing API token of AI profile '{}'", name.get(), e);
        }
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
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

    public StringProperty chatModelProperty() {
        return chatModel;
    }

    public String getChatModel() {
        return chatModel.get();
    }

    public void setChatModel(String chatModel) {
        this.chatModel.set(chatModel);
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

    public ObjectProperty<EmbeddingModel> embeddingModelProperty() {
        return embeddingModel;
    }

    public EmbeddingModel getEmbeddingModel() {
        if (getCustomizeExpertSettings()) {
            return embeddingModel.get();
        } else {
            return AiDefaultPreferences.EMBEDDING_MODEL;
        }
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel.set(embeddingModel);
    }

    public StringProperty apiBaseUrlProperty() {
        return apiBaseUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl.get();
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl.set(apiBaseUrl);
    }

    public StringProperty instructionProperty() {
        return instruction;
    }

    public String getInstruction() {
        if (getCustomizeExpertSettings()) {
            return instruction.get();
        } else {
            return AiDefaultPreferences.SYSTEM_MESSAGE;
        }
    }

    public void setInstruction(String instruction) {
        this.instruction.set(instruction);
    }

    public DoubleProperty temperatureProperty() {
        return temperature;
    }

    public double getTemperature() {
        if (getCustomizeExpertSettings()) {
            return temperature.get();
        } else {
            return AiDefaultPreferences.TEMPERATURE;
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
            return AiDefaultPreferences.getContextWindowSize(aiProvider.get(), chatModel.get());
        }
    }

    public void setContextWindowSize(int contextWindowSize) {
        this.contextWindowSize.set(contextWindowSize);
    }

    public IntegerProperty documentSplitterChunkSizeProperty() {
        return documentSplitterChunkSize;
    }

    public int getDocumentSplitterChunkSize() {
        if (getCustomizeExpertSettings()) {
            return documentSplitterChunkSize.get();
        } else {
            return AiDefaultPreferences.DOCUMENT_SPLITTER_CHUNK_SIZE;
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
            return AiDefaultPreferences.DOCUMENT_SPLITTER_OVERLAP;
        }
    }

    public void setDocumentSplitterOverlapSize(int documentSplitterOverlapSize) {
        this.documentSplitterOverlapSize.set(documentSplitterOverlapSize);
    }

    public IntegerProperty ragMaxResultsCountProperty() {
        return ragMaxResultsCount;
    }

    public int getRagMaxResultsCount() {
        if (getCustomizeExpertSettings()) {
            return ragMaxResultsCount.get();
        } else {
            return AiDefaultPreferences.RAG_MAX_RESULTS_COUNT;
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
            return AiDefaultPreferences.RAG_MIN_SCORE;
        }
    }

    public void setRagMinScore(double ragMinScore) {
        this.ragMinScore.set(ragMinScore);
    }
}
