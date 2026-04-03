package org.jabref.logic.ai.summarization.tasks.generatesummary;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public record GenerateSummaryTaskRequest(
        FilePreferences filePreferences,
        ChatModel chatModel,
        SummariesRepository summariesRepository,
        Summarizator summarizator,
        BibDatabaseContext bibDatabaseContext,
        BibEntry entry,
        boolean regenerate
) {
}
