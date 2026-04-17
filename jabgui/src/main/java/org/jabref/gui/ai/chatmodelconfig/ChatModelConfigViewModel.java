package org.jabref.gui.ai.chatmodelconfig;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
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
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.ChatModelFactory;
import org.jabref.logic.ai.preferences.AiDefaultExpertSettings;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.LocalizedNumbers;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.llm.PredefinedChatModel;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

/// ViewModel for the reusable chat-model configuration component.
///
/// Holds the full list of AI profiles (name, provider, chatModel, apiKey, apiBaseUrl)
/// plus the currently-selected profile index, and mirrors the selected profile's fields
/// into single-edit display properties for the form.
///
/// Seed via {@link #loadFrom(AiPreferences)}.
/// Persist via {@link #storeInto(AiPreferences)}.
public class ChatModelConfigViewModel extends AbstractViewModel {

    private final ObservableList<String> profileNames = FXCollections.observableArrayList();
    private final ObservableList<String> profileProviders = FXCollections.observableArrayList();
    private final ObservableList<String> profileChatModels = FXCollections.observableArrayList();
    private final ObservableList<String> profileApiBaseUrls = FXCollections.observableArrayList();

    private final ObservableList<String> profileApiKeys = FXCollections.observableArrayList();

    private final IntegerProperty selectedProfileIndex = new SimpleIntegerProperty(-1);
    private final ObjectProperty<AiProvider> selectedProvider = new SimpleObjectProperty<>();
    private final StringProperty currentChatModel = new SimpleStringProperty();
    private final StringProperty currentApiKey = new SimpleStringProperty();
    private final StringProperty currentApiBaseUrl = new SimpleStringProperty();

    private final ListProperty<AiProvider> providersList =
            new SimpleListProperty<>(FXCollections.observableArrayList(AiProvider.values()));

    private final ListProperty<String> chatModelsList =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    private final StringProperty temperature = new SimpleStringProperty(
            LocalizedNumbers.doubleToString(AiDefaultExpertSettings.TEMPERATURE));
    private final IntegerProperty contextWindowSize = new SimpleIntegerProperty(
            AiDefaultExpertSettings.CONTEXT_WINDOW_SIZE);

    private final ListProperty<TokenEstimatorKind> tokenEstimatorKindsList =
            new SimpleListProperty<>(FXCollections.observableArrayList(TokenEstimatorKind.values()));
    private final ObjectProperty<TokenEstimatorKind> selectedTokenEstimatorKind =
            new SimpleObjectProperty<>(AiDefaultExpertSettings.TOKEN_ESTIMATOR_KIND);

    private final BooleanProperty disableApiBaseUrl = new SimpleBooleanProperty(false);

    private ObjectBinding<ChatModel> chatModelBinding;

    // Guard to prevent index-switch listener from firing while we are loading all profiles
    private boolean loading = false;

    private final Validator apiKeyValidator;
    private final Validator chatModelValidator;
    private final Validator apiBaseUrlValidator;
    private final Validator temperatureTypeValidator;
    private final Validator temperatureRangeValidator;
    private final Validator contextWindowSizeValidator;

    public ChatModelConfigViewModel() {
        apiKeyValidator = new FunctionBasedValidator<>(
                currentApiKey,
                token -> !StringUtil.isBlank(token),
                ValidationMessage.error(Localization.lang("An API key has to be provided")));

        chatModelValidator = new FunctionBasedValidator<>(
                currentChatModel,
                model -> !StringUtil.isBlank(model),
                ValidationMessage.error(Localization.lang("Chat model has to be provided")));

        apiBaseUrlValidator = new FunctionBasedValidator<>(
                currentApiBaseUrl,
                url -> !StringUtil.isBlank(url),
                ValidationMessage.error(Localization.lang("API base URL has to be provided")));

        temperatureTypeValidator = new FunctionBasedValidator<>(
                temperature,
                temp -> LocalizedNumbers.stringToDouble(temp).isPresent(),
                ValidationMessage.error(Localization.lang("Temperature must be a number")));

        // Source: https://platform.openai.com/docs/api-reference/chat/create#chat-create-temperature
        temperatureRangeValidator = new FunctionBasedValidator<>(
                temperature,
                temp -> LocalizedNumbers.stringToDouble(temp).map(t -> t >= 0 && t <= 2).orElse(false),
                ValidationMessage.error(Localization.lang("Temperature must be between 0 and 2")));

        contextWindowSizeValidator = new FunctionBasedValidator<>(
                contextWindowSize,
                size -> size.intValue() > 0,
                ValidationMessage.error(Localization.lang("Context window size must be greater than 0")));

        setupListeners();
    }

    private void setupListeners() {
        // When selected profile index changes, load that profile's values into the edit fields.
        selectedProfileIndex.addListener((_, _, newIdx) -> {
            if (loading) {
                return;
            }
            int idx = newIdx.intValue();
            if (idx < 0 || idx >= profileProviders.size()) {
                return;
            }
            loadEditFieldsFromProfile(idx);
        });

        // Keep provider combo list & disableApiBaseUrl in sync with the selected provider.
        selectedProvider.addListener((_, _, newValue) -> {
            if (newValue == null) {
                return;
            }
            disableApiBaseUrl.set(newValue == AiProvider.HUGGING_FACE || newValue == AiProvider.GEMINI);
            List<String> models = PredefinedChatModel.getAvailableModels(newValue);
            chatModelsList.setAll(models);
        });

        // Auto-update context window size when a predefined model is selected.
        currentChatModel.addListener((_, _, newValue) -> {
            if (newValue == null) {
                return;
            }
            contextWindowSize.set(PredefinedChatModel.getContextWindowSize(selectedProvider.get(), newValue));
        });

        // When any edit field changes, sync back into the profile lists if not loading.
        selectedProvider.addListener((_, _, newVal) -> {
            if (!loading && newVal != null) {
                int idx = selectedProfileIndex.get();
                if (idx >= 0 && idx < profileProviders.size()) {
                    profileProviders.set(idx, newVal.name());
                }
            }
        });
        currentChatModel.addListener((_, _, newVal) -> {
            if (!loading && newVal != null) {
                int idx = selectedProfileIndex.get();
                if (idx >= 0 && idx < profileChatModels.size()) {
                    profileChatModels.set(idx, newVal);
                }
            }
        });
        currentApiKey.addListener((_, _, newVal) -> {
            if (!loading && newVal != null) {
                int idx = selectedProfileIndex.get();
                if (idx >= 0 && idx < profileApiKeys.size()) {
                    profileApiKeys.set(idx, newVal);
                }
            }
        });
        currentApiBaseUrl.addListener((_, _, newVal) -> {
            if (!loading && newVal != null) {
                int idx = selectedProfileIndex.get();
                if (idx >= 0 && idx < profileApiBaseUrls.size()) {
                    profileApiBaseUrls.set(idx, newVal);
                }
            }
        });

        // Build the reactive chat-model binding. The binding is lazy: it only recomputes
        // when observed AND one of its dependencies has changed.
        // NOTE: Do NOT add a close-old-model listener here. Ownership of each ChatModel is
        // held by the consumer (e.g. AiChatView), which is responsible for lifecycle cleanup.
        // Adding a second close listener here would cause a double-close when the consumer
        // also listens to the same property changes.
        chatModelBinding = Bindings.createObjectBinding(
                this::constructChatModel,
                selectedProvider, currentChatModel, currentApiKey, currentApiBaseUrl,
                temperature, contextWindowSize, selectedTokenEstimatorKind
        );
    }

    // --- load ---

    /// Seeds all profile lists from {@link AiPreferences} and selects the active profile.
    public void loadFrom(AiPreferences prefs) {
        loading = true;
        try {
            profileNames.setAll(prefs.getProfileNames());
            profileProviders.setAll(prefs.getProfileProviders());
            profileChatModels.setAll(prefs.getProfileChatModels());
            profileApiBaseUrls.setAll(prefs.getProfileApiBaseUrls());

            // Load API keys in-memory
            profileApiKeys.clear();
            for (int i = 0; i < profileProviders.size(); i++) {
                profileApiKeys.add(prefs.getApiKeyForProfile(i));
            }

            // Also load shared expert settings
            temperature.set(LocalizedNumbers.doubleToString(prefs.getTemperature()));
            contextWindowSize.set(prefs.getContextWindowSize());
            selectedTokenEstimatorKind.set(prefs.getTokenEstimatorKind());

            int idx = Math.clamp(prefs.getSelectedProfileIndex(), 0, profileProviders.size() - 1);
            selectedProfileIndex.set(idx);
            loadEditFieldsFromProfile(idx);
        } finally {
            loading = false;
        }
    }

    private static AiProvider aiProviderFromString(String name) {
        try {
            return AiProvider.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return AiProvider.OPEN_AI;
        }
    }

    private void loadEditFieldsFromProfile(int idx) {
        if (idx < 0 || idx >= profileProviders.size()) {
            return;
        }

        loading = true;

        try {
            AiProvider provider = aiProviderFromString(profileProviders.get(idx));

            chatModelsList.setAll(PredefinedChatModel.getAvailableModels(provider));
            currentChatModel.set(idx < profileChatModels.size() ? profileChatModels.get(idx) : "");
            currentApiKey.set(idx < profileApiKeys.size() ? profileApiKeys.get(idx) : "");
            currentApiBaseUrl.set(idx < profileApiBaseUrls.size() ? profileApiBaseUrls.get(idx) : "");
            selectedProvider.set(provider);
            disableApiBaseUrl.set(provider == AiProvider.HUGGING_FACE || provider == AiProvider.GEMINI);
        } finally {
            loading = false;
        }
    }

    /// Writes all profile lists and selected index back to {@link AiPreferences}.
    /// Also stores API keys in the keyring via {@link AiPreferences#storeApiKeyForProfile}.
    /// Call this from {@code AiTab.storeSettings()}.
    public void storeInto(AiPreferences prefs) {
        int idx = selectedProfileIndex.get();
        if (idx >= 0 && idx < profileProviders.size()) {
            if (selectedProvider.get() != null) {
                profileProviders.set(idx, selectedProvider.get().name());
            }
            profileChatModels.set(idx, currentChatModel.get() == null ? "" : currentChatModel.get());
            profileApiBaseUrls.set(idx, currentApiBaseUrl.get() == null ? "" : currentApiBaseUrl.get());
            profileApiKeys.set(idx, currentApiKey.get() == null ? "" : currentApiKey.get());
        }

        prefs.getProfileNames().setAll(profileNames);
        prefs.getProfileProviders().setAll(profileProviders);
        prefs.getProfileChatModels().setAll(profileChatModels);
        prefs.getProfileApiBaseUrls().setAll(profileApiBaseUrls);
        prefs.setSelectedProfileIndex(selectedProfileIndex.get());

        for (int i = 0; i < profileApiKeys.size(); i++) {
            prefs.storeApiKeyForProfile(i, profileApiKeys.get(i));
        }

        double temp = LocalizedNumbers.stringToDouble(temperature.get())
                                      .orElse((double) AiDefaultExpertSettings.TEMPERATURE);
        prefs.setTemperature(temp);
        prefs.setContextWindowSize(contextWindowSize.get());
        prefs.setTokenEstimatorKind(selectedTokenEstimatorKind.get());

        prefs.apiKeyUpdated();
    }

    /// Adds a new profile with a unique name and defaults to the first available provider/model.
    public void addProfile() {
        String name = generateProfileName();
        AiProvider provider = AiProvider.OPEN_AI;
        String model = PredefinedChatModel.getAvailableModels(provider).stream().findFirst().orElse("");
        profileNames.add(name);
        profileProviders.add(provider.name());
        profileChatModels.add(model);
        profileApiBaseUrls.add(provider.getApiUrl());
        profileApiKeys.add("");
        // Select the newly added profile
        selectedProfileIndex.set(profileNames.size() - 1);
    }

    /// Removes the profile at the given index. At least one profile is always kept.
    public void removeProfile(int index) {
        if (profileNames.size() <= 1 || index < 0 || index >= profileNames.size()) {
            return;
        }
        profileNames.remove(index);
        profileProviders.remove(index);
        profileChatModels.remove(index);
        profileApiBaseUrls.remove(index);
        profileApiKeys.remove(index);
        int newIdx = Math.min(selectedProfileIndex.get(), profileNames.size() - 1);
        selectedProfileIndex.set(newIdx);
    }

    public void renameCurrentProfile(String newName) {
        int idx = selectedProfileIndex.get();
        if (idx >= 0 && idx < profileNames.size() && !StringUtil.isBlank(newName)) {
            profileNames.set(idx, newName.trim());
        }
    }

    private String generateProfileName() {
        // TODO: Simplify.

        List<String> existing = new ArrayList<>(profileNames);
        int n = existing.size() + 1;
        String candidate;
        do {
            candidate = Localization.lang("Profile %0", n++);
        } while (existing.contains(candidate));
        return candidate;
    }

    private ChatModel constructChatModel() {
        AiProvider provider = selectedProvider.get() != null ? selectedProvider.get() : AiProvider.OPEN_AI;

        String apiKey = currentApiKey.get();
        if (StringUtil.isBlank(apiKey)) {
            apiKey = "";
        }

        String modelName = currentChatModel.get();
        if (StringUtil.isBlank(modelName)) {
            // Fall back to the first predefined model for the provider.
            modelName = PredefinedChatModel.getAvailableModels(provider)
                                           .stream()
                                           .findFirst()
                                           .orElse("");
        }

        double temp = LocalizedNumbers.stringToDouble(temperature.get())
                                      .orElse((double) AiDefaultExpertSettings.TEMPERATURE);
        String baseUrl = currentApiBaseUrl.get() == null ? "" : currentApiBaseUrl.get();

        return ChatModelFactory.create(
                provider,
                modelName,
                apiKey,
                temp,
                baseUrl,
                contextWindowSize.get(),
                selectedTokenEstimatorKind.get()
        );
    }

    public void resetToDefaults() {
        if (selectedProvider.get() != null) {
            currentApiBaseUrl.set(selectedProvider.get().getApiUrl());
        }

        contextWindowSize.set(
                PredefinedChatModel.getContextWindowSize(selectedProvider.get(), currentChatModel.get()));
        temperature.set(LocalizedNumbers.doubleToString(AiDefaultExpertSettings.TEMPERATURE));
    }

    public boolean validate() {
        return getValidators().stream()
                              .map(Validator::getValidationStatus)
                              .allMatch(ValidationStatus::isValid);
    }

    public List<Validator> getValidators() {
        return List.of(
                apiKeyValidator,
                chatModelValidator,
                apiBaseUrlValidator,
                temperatureTypeValidator,
                temperatureRangeValidator,
                contextWindowSizeValidator
        );
    }

    public ObservableList<String> profileNamesProperty() {
        return profileNames;
    }

    public IntegerProperty selectedProfileIndexProperty() {
        return selectedProfileIndex;
    }

    public ReadOnlyListProperty<AiProvider> providersProperty() {
        return providersList;
    }

    public ObjectProperty<AiProvider> selectedProviderProperty() {
        return selectedProvider;
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

    public StringProperty apiBaseUrlProperty() {
        return currentApiBaseUrl;
    }

    public BooleanProperty disableApiBaseUrlProperty() {
        return disableApiBaseUrl;
    }

    public StringProperty temperatureProperty() {
        return temperature;
    }

    public IntegerProperty contextWindowSizeProperty() {
        return contextWindowSize;
    }

    public ReadOnlyListProperty<TokenEstimatorKind> tokenEstimatorKindsProperty() {
        return tokenEstimatorKindsList;
    }

    public ObjectProperty<TokenEstimatorKind> selectedTokenEstimatorKindProperty() {
        return selectedTokenEstimatorKind;
    }

    public ObservableValue<ChatModel> chatModelProperty() {
        return chatModelBinding;
    }
}















