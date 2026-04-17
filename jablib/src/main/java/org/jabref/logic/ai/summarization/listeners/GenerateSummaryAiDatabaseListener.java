package org.jabref.logic.ai.summarization.listeners;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiDatabaseListener;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.ChatModelFactory;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.logic.SummarizatorFactory;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.tasks.generatesummary.GenerateSummaryTaskRequest;
import org.jabref.logic.util.ObservablesHelper;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateSummaryAiDatabaseListener implements AiDatabaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryAiDatabaseListener.class);

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final SummarizationTaskAggregator summarizationTaskAggregator;

    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<Summarizator> summarizator = new SimpleObjectProperty<>();

    public GenerateSummaryAiDatabaseListener(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            SummarizationTaskAggregator summarizationTaskAggregator
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.summarizationTaskAggregator = summarizationTaskAggregator;

        setupBindings();
    }

    private void setupBindings() {
        this.summarizator.bind(ObservablesHelper.createObjectBinding(
                () -> SummarizatorFactory.create(aiPreferences),
                aiPreferences.getSummarizatorProperties()
        ));

        this.chatModel.bind(ObservablesHelper.createObjectBinding(
                () -> ChatModelFactory.create(aiPreferences),
                aiPreferences.getChatProperties()
        ));
    }

    @Override
    public void setupDatabase(BibDatabaseContext context) {
        // GC was eating the listeners, so we have to fall back to the event bus.
        context.getDatabase().registerListener(new EntriesChangedListener(context));
    }

    @Override
    public void close() throws Exception {
        if (chatModel instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    private class EntriesChangedListener {
        private final BibDatabaseContext context;

        public EntriesChangedListener(BibDatabaseContext context) {
            this.context = context;
        }

        @Subscribe
        public void listen(EntriesAddedEvent e) {
            e.getBibEntries().forEach(entry -> {
                if (aiPreferences.getAutoGenerateSummaries()) {
                    summarizationTaskAggregator.start(new GenerateSummaryTaskRequest(
                            filePreferences,
                            chatModel.get(),
                            summarizator.get(),
                            new FullBibEntry(context, entry),
                            false
                    ));
                }
            });
        }

        @Subscribe
        public void listen(FieldChangedEvent e) {
            if (e.getField() == StandardField.FILE && aiPreferences.getAutoGenerateSummaries()) {
                summarizationTaskAggregator.start(new GenerateSummaryTaskRequest(
                        filePreferences,
                        chatModel.get(),
                        summarizator.get(),
                        new FullBibEntry(context, e.getBibEntry()),
                        false
                ));
            }
        }
    }
}
