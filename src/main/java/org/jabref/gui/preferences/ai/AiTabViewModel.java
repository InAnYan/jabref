package org.jabref.gui.preferences.ai;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.AiPreferences;
import org.jabref.preferences.PreferencesService;

import com.dlsc.unitfx.DoubleInputField;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class AiTabViewModel implements PreferenceTabViewModel {
    private final BooleanProperty useAi = new SimpleBooleanProperty();
    private final StringProperty openAiToken = new SimpleStringProperty();
    private final StringProperty systemMessage = new SimpleStringProperty();
    private final IntegerProperty messageWindowSize = new SimpleIntegerProperty();
    private final IntegerProperty documentSplitterChunkSize = new SimpleIntegerProperty();
    private final IntegerProperty documentSplitterOverlapSize = new SimpleIntegerProperty();
    private final IntegerProperty ragMaxResultsCount = new SimpleIntegerProperty();
    private final DoubleProperty ragMinScore = new SimpleDoubleProperty();

    private final AiPreferences aiPreferences;

    private final Validator openAiTokenValidator;
    private final Validator messageWindowSizeValidator;
    private final Validator documentSplitterChunkSizeValidator;
    private final Validator documentSplitterOverlapSizeValidator;
    private final Validator ragMaxResultsCountValidator;
    private final Validator ragMinScoreValidator;

    public AiTabViewModel(PreferencesService preferencesService) {
        this.aiPreferences = preferencesService.getAiPreferences();

        this.openAiTokenValidator = new FunctionBasedValidator<>(
                openAiToken,
                (token) -> !StringUtil.isBlank(token),
                ValidationMessage.error(Localization.lang("The OpenAI token cannot be empty")));

        this.messageWindowSizeValidator = new FunctionBasedValidator<>(
                messageWindowSize,
                (size) -> (int)size > 0,
                ValidationMessage.error(Localization.lang("Message window size must be greater than 0")));

        this.documentSplitterChunkSizeValidator = new FunctionBasedValidator<>(
                documentSplitterChunkSize,
                (size) -> (int)size > 0,
                ValidationMessage.error(Localization.lang("Document splitter chunk size must be greater than 0")));

        this.documentSplitterOverlapSizeValidator = new FunctionBasedValidator<>(
                documentSplitterOverlapSize,
                (size) -> (int)size > 0 && (int)size < documentSplitterChunkSize.get(),
                ValidationMessage.error(Localization.lang("Document splitter overlap size must be greater than 0 and less than chunk size")));

        this.ragMaxResultsCountValidator = new FunctionBasedValidator<>(
                ragMaxResultsCount,
                (count) -> (int)count > 0,
                ValidationMessage.error(Localization.lang("RAG max results count must be greater than 0")));

        this.ragMinScoreValidator = new FunctionBasedValidator<>(
                ragMinScore,
                (score) -> (double)score > 0 && (double)score < 1,
                ValidationMessage.error(Localization.lang("RAG min score must be greater than 0 and less than 1")));
    }

    @Override
    public void setValues() {
        useAi.setValue(aiPreferences.getEnableChatWithFiles());
        openAiToken.setValue(aiPreferences.getOpenAiToken());

        systemMessage.setValue(aiPreferences.getSystemMessage());
        messageWindowSize.setValue(aiPreferences.getMessageWindowSize());
        documentSplitterChunkSize.setValue(aiPreferences.getDocumentSplitterChunkSize());
        documentSplitterOverlapSize.setValue(aiPreferences.getDocumentSplitterOverlapSize());
        ragMaxResultsCount.setValue(aiPreferences.getRagMaxResultsCount());
        ragMinScore.setValue(aiPreferences.getRagMinScore());
    }

    @Override
    public void storeSettings() {
        aiPreferences.setEnableChatWithFiles(useAi.get());
        aiPreferences.setOpenAiToken(openAiToken.get());

        aiPreferences.setSystemMessage(systemMessage.get());
        aiPreferences.setMessageWindowSize(messageWindowSize.get());
        aiPreferences.setDocumentSplitterChunkSize(documentSplitterChunkSize.get());
        aiPreferences.setDocumentSplitterOverlapSize(documentSplitterOverlapSize.get());
        aiPreferences.setRagMaxResultsCount(ragMaxResultsCount.get());
        aiPreferences.setRagMinScore(ragMinScore.get());
    }

    @Override
    public boolean validateSettings() {
        return openAiTokenValidator.getValidationStatus().isValid() && messageWindowSizeValidator.getValidationStatus().isValid() && documentSplitterChunkSizeValidator.getValidationStatus().isValid() && documentSplitterOverlapSizeValidator.getValidationStatus().isValid() && ragMaxResultsCountValidator.getValidationStatus().isValid() && ragMinScoreValidator.getValidationStatus().isValid();
    }

    public StringProperty openAiTokenProperty() {
        return openAiToken;
    }

    public BooleanProperty useAiProperty() {
        return useAi;
    }

    public StringProperty systemMessageProperty() {
        return systemMessage;
    }

    public IntegerProperty messageWindowSizeProperty() {
        return messageWindowSize;
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

    public DoubleProperty ragMinScoreProperty() {
        return ragMinScore;
    }
}
