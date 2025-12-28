package org.jabref.logic.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChattingAiFeature;
import org.jabref.logic.ai.citationparsing.CitationParsingAiFeature;
import org.jabref.logic.ai.embedding.EmbeddingAiFeature;
import org.jabref.logic.ai.ingestion.IngestionAiFeature;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.RagAiFeature;
import org.jabref.logic.ai.summarization.SummarizationAiFeature;
import org.jabref.logic.ai.templates.TemplatesAiFeature;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * The main class for the AI functionality.
 * <p>
 * Holds all the AI components: LLM and embedding model, chat history and embedding cache.
 */
public class AiService implements AutoCloseable {
    public static final String VERSION = "2";

    // This field is used to shut down AI-related background tasks.
    // If a background task processes a big document and has a loop, then the task should check the status
    // of this property for being true. If it's true, then it should abort the cycle.
    private final BooleanProperty shutdownSignal = new SimpleBooleanProperty(false);

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-retrieval-pool-%d").build()
    );

    private final TemplatesAiFeature templatesFeature;
    private final EmbeddingAiFeature embeddingFeature;
    private final ChattingAiFeature chattingFeature;
    private final IngestionAiFeature ingestionFeature;
    private final RagAiFeature ragFeature;
    private final SummarizationAiFeature summarizationFeature;
    private final CitationParsingAiFeature citationParsingFeature;

    public AiService(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    ) {
        this.templatesFeature = new TemplatesAiFeature(aiPreferences);

        this.embeddingFeature = new EmbeddingAiFeature(
                aiPreferences,
                notificationService,
                taskExecutor
        );

        this.chattingFeature = new ChattingAiFeature(
                aiPreferences,
                notificationService
        );

        this.ingestionFeature = new IngestionAiFeature(
                aiPreferences,
                taskExecutor,
                notificationService
        );

        this.ragFeature = new RagAiFeature(
                aiPreferences,
                filePreferences,
                embeddingFeature.getCurrentEmbeddingModel(),
                ingestionFeature.getEmbeddingsStore()
        );

        this.summarizationFeature = new SummarizationAiFeature(
                aiPreferences,
                filePreferences,
                chattingFeature.getCurrentChatModel(),
                templatesFeature.getCurrentAiTemplates(),
                shutdownSignal,
                taskExecutor,
                notificationService
        );

        this.citationParsingFeature = new CitationParsingAiFeature();
    }

    public void setupDatabase(BibDatabaseContext context) {
        templatesFeature.setupDatabase(context);
        embeddingFeature.setupDatabase(context);
        chattingFeature.setupDatabase(context);
        ingestionFeature.setupDatabase(context);
        ragFeature.setupDatabase(context);
        summarizationFeature.setupDatabase(context);
        citationParsingFeature.setupDatabase(context);
    }

    public TemplatesAiFeature getTemplatesFeature() {
        return templatesFeature;
    }

    public EmbeddingAiFeature getEmbeddingFeature() {
        return embeddingFeature;
    }

    public ChattingAiFeature getChattingFeature() {
        return chattingFeature;
    }

    public IngestionAiFeature getIngestionFeature() {
        return ingestionFeature;
    }

    public RagAiFeature getRagFeature() {
        return ragFeature;
    }

    public SummarizationAiFeature getSummarizationFeature() {
        return summarizationFeature;
    }

    public CitationParsingAiFeature getCitationParsingFeature() {
        return citationParsingFeature;
    }

    public ReadOnlyBooleanProperty getShutdownSignal() {
        return shutdownSignal;
    }

    @Override
    public void close() throws Exception {
        shutdownSignal.set(true);
        cachedThreadPool.shutdownNow();

        citationParsingFeature.close();
        summarizationFeature.close();
        ragFeature.close();
        ingestionFeature.close();
        chattingFeature.close();
        embeddingFeature.close();
        templatesFeature.close();
    }
}
