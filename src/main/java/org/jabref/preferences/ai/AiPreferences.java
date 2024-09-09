package org.jabref.preferences.ai;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.ai.AiDefaultPreferences;

public class AiPreferences {
    private final BooleanProperty enableAi;

    private final ListProperty<AiProfile> profiles;
    private final OptionalObjectProperty<AiProfile> currentProfile;

    // If the current profile is empty, then all of those properties below are null.

    private final ObjectProperty<AiProvider> currentAiProvider = new SimpleObjectProperty<>();
    private final StringProperty currentChatModel = new SimpleStringProperty();

    private final BooleanProperty currentCustomizeExpertSettings = new SimpleBooleanProperty();

    private final StringProperty currentApiBaseUrl = new SimpleStringProperty();
    private final ObjectProperty<EmbeddingModel> currentEmbeddingModel = new SimpleObjectProperty<>();
    private final StringProperty currentInstruction = new SimpleStringProperty();
    private final DoubleProperty currentTemperature = new SimpleDoubleProperty();
    private final IntegerProperty currentContextWindowSize = new SimpleIntegerProperty();
    private final IntegerProperty currentDocumentSplitterChunkSize = new SimpleIntegerProperty();
    private final IntegerProperty currentDocumentSplitterOverlapSize = new SimpleIntegerProperty();
    private final IntegerProperty currentRagMaxResultsCount = new SimpleIntegerProperty();
    private final DoubleProperty currentRagMinScore = new SimpleDoubleProperty();

    public AiPreferences(boolean enableAi, List<AiProfile> aiProfiles, Optional<AiProfile> currentProfile) {
        this.enableAi = new SimpleBooleanProperty(enableAi);

        this.profiles = new SimpleListProperty<>(FXCollections.observableArrayList(aiProfiles));
        this.currentProfile = new OptionalObjectProperty<>(currentProfile);

        if (currentProfile.isPresent()) {
            setCurrentProperties();
        }

        this.currentProfile.addListener((observable, oldValue, newValue) -> {
            if (newValue.isPresent()) {
                setCurrentProperties();
            }
        });
    }

    private void setCurrentProperties() {
        currentProfile.get().ifPresent(currentProfile -> {
            currentAiProvider.set(currentProfile.getAiProvider());
            currentChatModel.set(currentProfile.getChatModel());

            currentCustomizeExpertSettings.set(currentProfile.getCustomizeExpertSettings());

            currentApiBaseUrl.set(currentProfile.getApiBaseUrl());
            currentEmbeddingModel.set(currentProfile.getEmbeddingModel());
            currentInstruction.set(currentProfile.getInstruction());
            currentTemperature.set(currentProfile.getTemperature());
            currentContextWindowSize.set(currentProfile.getContextWindowSize());
            currentDocumentSplitterChunkSize.set(currentProfile.getDocumentSplitterChunkSize());
            currentDocumentSplitterOverlapSize.set(currentProfile.getDocumentSplitterOverlapSize());
            currentRagMaxResultsCount.set(currentProfile.getRagMaxResultsCount());
        });
    }

    public String loadApiKey() {
        return currentProfile.get().map(AiProfile::loadApiKey).orElse("");
    }

    public void storeApikey(String newKey) {
        currentProfile.get().ifPresent(profile -> profile.storeApiKey(newKey));
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

    public ListProperty<AiProfile> profilesProperty() {
        return profiles;
    }

    public List<AiProfile> getProfiles() {
        return profiles.get();
    }

    public void setProfiles(List<AiProfile> profiles) {
        this.profiles.set(FXCollections.observableArrayList(profiles));
    }

    public OptionalObjectProperty<AiProfile> currentProfileProperty() {
        return currentProfile;
    }

    public Optional<AiProfile> getCurrentProfile() {
        return currentProfile.get();
    }

    public void setCurrentProfile(Optional<AiProfile> currentProfile) {
        this.currentProfile.set(currentProfile);
        setCurrentProperties();
    }

    public ObjectProperty<AiProvider> aiProviderProperty() {
        return currentAiProvider;
    }

    public AiProvider getAiProvider() {
        return currentAiProvider.get();
    }

    public void setAiProvider(AiProvider aiProvider) {
        this.currentAiProvider.set(aiProvider);
    }

    public StringProperty chatModelProperty() {
        return currentChatModel;
    }

    public String getChatModel() {
        return currentChatModel.get();
    }

    public void setChatModel(String chatModel) {
        this.currentChatModel.set(chatModel);
    }

    public BooleanProperty customizeExpertSettingsProperty() {
        return currentCustomizeExpertSettings;
    }

    public boolean getCustomizeExpertSettings() {
        return currentCustomizeExpertSettings.get();
    }

    public void setCustomizeExpertSettings(boolean customizeExpertSettings) {
        this.currentCustomizeExpertSettings.set(customizeExpertSettings);
    }

    public ObjectProperty<EmbeddingModel> embeddingModelProperty() {
        return currentEmbeddingModel;
    }

    public EmbeddingModel getEmbeddingModel() {
        if (getCustomizeExpertSettings()) {
            return currentEmbeddingModel.get();
        } else {
            return AiDefaultPreferences.EMBEDDING_MODEL;
        }
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.currentEmbeddingModel.set(embeddingModel);
    }

    public StringProperty apiBaseUrlProperty() {
        return currentApiBaseUrl;
    }

    public String getApiBaseUrl() {
        return currentApiBaseUrl.get();
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.currentApiBaseUrl.set(apiBaseUrl);
    }

    public StringProperty instructionProperty() {
        return currentInstruction;
    }

    public String getInstruction() {
        if (getCustomizeExpertSettings()) {
            return currentInstruction.get();
        } else {
            return AiDefaultPreferences.SYSTEM_MESSAGE;
        }
    }

    public void setInstruction(String instruction) {
        this.currentInstruction.set(instruction);
    }

    public DoubleProperty temperatureProperty() {
        return currentTemperature;
    }

    public double getTemperature() {
        if (getCustomizeExpertSettings()) {
            return currentTemperature.get();
        } else {
            return AiDefaultPreferences.TEMPERATURE;
        }
    }

    public void setTemperature(double temperature) {
        this.currentTemperature.set(temperature);
    }

    public IntegerProperty contextWindowSizeProperty() {
        return currentContextWindowSize;
    }

    public int getContextWindowSize() {
        if (getCustomizeExpertSettings()) {
            return currentContextWindowSize.get();
        } else {
            return AiDefaultPreferences.getContextWindowSize(currentAiProvider.get(), currentChatModel.get());
        }
    }

    public void setContextWindowSize(int contextWindowSize) {
        this.currentContextWindowSize.set(contextWindowSize);
    }

    public IntegerProperty documentSplitterChunkSizeProperty() {
        return currentDocumentSplitterChunkSize;
    }

    public int getDocumentSplitterChunkSize() {
        if (getCustomizeExpertSettings()) {
            return currentDocumentSplitterChunkSize.get();
        } else {
            return AiDefaultPreferences.DOCUMENT_SPLITTER_CHUNK_SIZE;
        }
    }

    public void setDocumentSplitterChunkSize(int documentSplitterChunkSize) {
        this.currentDocumentSplitterChunkSize.set(documentSplitterChunkSize);
    }

    public IntegerProperty documentSplitterOverlapSizeProperty() {
        return currentDocumentSplitterOverlapSize;
    }

    public int getDocumentSplitterOverlapSize() {
        if (getCustomizeExpertSettings()) {
            return currentDocumentSplitterOverlapSize.get();
        } else {
            return AiDefaultPreferences.DOCUMENT_SPLITTER_OVERLAP;
        }
    }

    public void setDocumentSplitterOverlapSize(int documentSplitterOverlapSize) {
        this.currentDocumentSplitterOverlapSize.set(documentSplitterOverlapSize);
    }

    public IntegerProperty ragMaxResultsCountProperty() {
        return currentRagMaxResultsCount;
    }

    public int getRagMaxResultsCount() {
        if (getCustomizeExpertSettings()) {
            return currentRagMaxResultsCount.get();
        } else {
            return AiDefaultPreferences.RAG_MAX_RESULTS_COUNT;
        }
    }

    public void setRagMaxResultsCount(int ragMaxResultsCount) {
        this.currentRagMaxResultsCount.set(ragMaxResultsCount);
    }

    public DoubleProperty ragMinScoreProperty() {
        return currentRagMinScore;
    }

    public double getRagMinScore() {
        if (getCustomizeExpertSettings()) {
            return currentRagMinScore.get();
        } else {
            return AiDefaultPreferences.RAG_MIN_SCORE;
        }
    }

    public void setRagMinScore(double ragMinScore) {
        this.currentRagMinScore.set(ragMinScore);
    }

    /**
     * Listen to changes of preferences that are related to embeddings generation.
     *
     * @param runnable The runnable that should be executed when the preferences change.
     */
    public void addListenerToEmbeddingsParametersChange(Runnable runnable) {
        currentEmbeddingModel.addListener((observableValue, oldValue, newValue) -> {
            if (newValue != oldValue) {
                runnable.run();
            }
        });

        currentDocumentSplitterChunkSize.addListener((observableValue, oldValue, newValue) -> {
            if (!Objects.equals(newValue, oldValue)) {
                runnable.run();
            }
        });

        currentDocumentSplitterOverlapSize.addListener((observableValue, oldValue, newValue) -> {
            if (!Objects.equals(newValue, oldValue)) {
                runnable.run();
            }
        });
    }
}
