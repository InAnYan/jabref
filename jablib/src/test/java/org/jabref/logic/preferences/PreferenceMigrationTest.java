package org.jabref.logic.preferences;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jabref.model.ai.pipeline.ResponseEngineKind;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class PreferenceMigrationTest {
    private static final String AI_ANSWER_ENGINE_KIND = "aiAnswerEngineKind";
    private static final String AI_RESPONSE_ENGINE_KIND = "aiResponseEngineKind";
    private static final String AI_EMBEDDING_MODEL = "aiEmbeddingModel";
    private static final String UNUSED_DEFAULT_VALUE = "";

    private Preferences testNode;
    private JabRefCliPreferences preferences;

    @BeforeEach
    void setUp() {
        testNode = Preferences.userRoot().node("/org/jabref/test-" + UUID.randomUUID());
        preferences = new JabRefCliPreferences(testNode);
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        testNode.removeNode();
        testNode.flush();
    }

    @Test
    void getAiPreferencesMigratesLegacyResponseEngineKind() {
        preferences.put(AI_ANSWER_ENGINE_KIND, ResponseEngineKind.FULL_DOCUMENT.name());

        ResponseEngineKind responseEngineKind = preferences.getAiPreferences().getResponseEngineKind();

        assertEquals(ResponseEngineKind.FULL_DOCUMENT, responseEngineKind);
        assertEquals(ResponseEngineKind.FULL_DOCUMENT.name(), preferences.get(AI_RESPONSE_ENGINE_KIND, UNUSED_DEFAULT_VALUE));
    }

    @Test
    void getAiPreferencesKeepsNewResponseEngineKindWhenLegacyValueExists() {
        preferences.put(AI_ANSWER_ENGINE_KIND, ResponseEngineKind.FULL_DOCUMENT.name());
        preferences.put(AI_RESPONSE_ENGINE_KIND, ResponseEngineKind.EMBEDDINGS_SEARCH.name());

        ResponseEngineKind responseEngineKind = preferences.getAiPreferences().getResponseEngineKind();

        assertEquals(ResponseEngineKind.EMBEDDINGS_SEARCH, responseEngineKind);
        assertEquals(ResponseEngineKind.EMBEDDINGS_SEARCH.name(), preferences.get(AI_RESPONSE_ENGINE_KIND, UNUSED_DEFAULT_VALUE));
    }

    @Test
    void getAiPreferencesMigratesLegacyEmbeddingModelL12() {
        preferences.put(AI_EMBEDDING_MODEL, "SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V2");

        String embeddingModel = preferences.getAiPreferences().getEmbeddingModel();

        assertEquals("sentence-transformers/all-MiniLM-L12-v2", embeddingModel);
        assertEquals("sentence-transformers/all-MiniLM-L12-v2", preferences.get(AI_EMBEDDING_MODEL, UNUSED_DEFAULT_VALUE));
    }

    @Test
    void getAiPreferencesMigratesLegacyEmbeddingModelL6() {
        preferences.put(AI_EMBEDDING_MODEL, "SENTENCE_TRANSFORMERS_ALL_MINILM_L6_V2");

        String embeddingModel = preferences.getAiPreferences().getEmbeddingModel();

        assertEquals("sentence-transformers/all-MiniLM-L6-v2", embeddingModel);
        assertEquals("sentence-transformers/all-MiniLM-L6-v2", preferences.get(AI_EMBEDDING_MODEL, UNUSED_DEFAULT_VALUE));
    }

    @Test
    void getAiPreferencesMigratesOtherLegacyEmbeddingModelToDefault() {
        preferences.put(AI_EMBEDDING_MODEL, "BAAI_BGE_LARGE_EN_V1_5");

        String embeddingModel = preferences.getAiPreferences().getEmbeddingModel();

        assertEquals("sentence-transformers/all-MiniLM-L12-v2", embeddingModel);
        assertEquals("sentence-transformers/all-MiniLM-L12-v2", preferences.get(AI_EMBEDDING_MODEL, UNUSED_DEFAULT_VALUE));
    }
}
