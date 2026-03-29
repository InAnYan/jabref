package org.jabref.logic.ai;

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

/**
 * The main class for the AI functionality.
 * <p>
 * Holds all the AI components: LLM and embedding model, chat history and embedding cache.
 */
public class AiService implements AutoCloseable {
    public static final String VERSION = "2";

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

    @Override
    public void close() throws Exception {

        citationParsingFeature.close();
        summarizationFeature.close();
        ragFeature.close();
        ingestionFeature.close();
        chattingFeature.close();
        embeddingFeature.close();
        templatesFeature.close();
    }
}
