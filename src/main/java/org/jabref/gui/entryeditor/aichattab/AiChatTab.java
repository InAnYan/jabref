package org.jabref.gui.entryeditor.aichattab;

import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.*;

import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.entryeditor.aichattab.components.AiChatComponentOld;
import org.jabref.gui.entryeditor.aichattab.components.aichat.AiChatComponent;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiChat;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.AiIngestor;
import org.jabref.logic.ai.ChatMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.AiPreferences;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.LoggerFactory;

public class AiChatTab extends EntryEditorTab {
    public static final String NAME = "AI chat";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AiChatTab.class.getName());

    private static final String QA_SYSTEM_MESSAGE = """
            You are an AI research assistant. You read and analyze scientific articles.
            The user will send you a question regarding a paper. You will be supplied also with the relevant information found in the article.
            Answer the question only by using the relevant information. Don't make up the answer.
            If you can't answer the user question using the provided information, then reply that you couldn't do it.""";

    private final FilePreferences filePreferences;
    private final AiPreferences aiPreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;

    private AiChatComponent aiChatComponent = null;

    private final AiService aiService;

    private AiChat aiChat = null;

    private BibEntry currentBibEntry = null;

    public AiChatTab(PreferencesService preferencesService, AiService aiService,
                     BibDatabaseContext bibDatabaseContext, TaskExecutor taskExecutor) {
        this.filePreferences = preferencesService.getFilePreferences();
        this.aiPreferences = preferencesService.getAiPreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();

        this.aiService = aiService;

        this.bibDatabaseContext = bibDatabaseContext;

        this.taskExecutor = taskExecutor;

        setText(Localization.lang(NAME));
        setTooltip(new Tooltip(Localization.lang("AI chat with full-text article")));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (!aiPreferences.getEnableChatWithFiles()) {
            setContent(new Label(Localization.lang("JabRef uses OpenAI to enable \"chatting\" with PDF files. OpenAI is an external service. To enable JabRef chatgting with PDF files, the content of the PDF files need to be shared with OpenAI. As soon as you ask a question, the text content of all PDFs attached to the entry are send to OpenAI. The privacy policy of OpenAI applies. You find it at <https://openai.com/policies/privacy-policy/>.")));
        } else if (entry.getCitationKey().isEmpty()) {
            setContent(new Label(Localization.lang("Please provide a citation key for the entry in order to enable chatting with PDF files.")));
        } else if (!checkIfCitationKeyIsUnique(bibDatabaseContext, entry.getCitationKey().get())) {
            setContent(new Label(Localization.lang("Please provide a unique citation key for the entry in order to enable chatting with PDF files.")));
        } else if (entry.getFiles().isEmpty()) {
            setContent(new Label(Localization.lang("No files attached")));
        } else if (!entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).allMatch(FileUtil::isPDFFile)) {
            setContent(new Label(Localization.lang("Only PDF files are supported")));
        } else {
            bindToCorrectEntry(entry);
        }
    }

    private static boolean checkIfCitationKeyIsUnique(BibDatabaseContext bibDatabaseContext, String citationKey) {
        return bibDatabaseContext.getDatabase().getEntries().stream()
              .map(BibEntry::getCitationKey)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .filter(key -> key.equals(citationKey))
              .count() == 1;
    }

    private void bindToCorrectEntry(BibEntry entry) {
        currentBibEntry = entry;

        createAiChat();
        aiChat.restoreMessages(entry.getAiChatMessages());
        ingestFiles(entry);
        buildChatUI(entry);
    }

    private void createAiChat() {
        aiChat = new AiChat(aiService, MetadataFilterBuilder.metadataKey("linkedFile").isIn(currentBibEntry.getFiles().stream().map(LinkedFile::getLink).toList()));
        aiChat.setSystemMessage(QA_SYSTEM_MESSAGE);
    }

    private void ingestFiles(BibEntry entry) {
        AiIngestor aiIngestor = new AiIngestor(aiService.getEmbeddingStore(), aiService.getEmbeddingModel());
        entry.getFiles().forEach(file -> {
            aiIngestor.ingestLinkedFile(file, bibDatabaseContext, filePreferences);
        });
    }

    private void buildChatUI(BibEntry entry) {
        aiChatComponent = new AiChatComponent((userPrompt) -> {
            ChatMessage userMessage = ChatMessage.user(userPrompt);
            aiChatComponent.addMessage(userMessage);
            entry.getAiChatMessages().add(userMessage);
            aiChatComponent.setLoading(true);

            BackgroundTask.wrap(() -> aiChat.execute(userPrompt))
                    .onSuccess(aiMessageText -> {
                        aiChatComponent.setLoading(false);

                        ChatMessage aiMessage = ChatMessage.assistant(aiMessageText);
                        aiChatComponent.addMessage(aiMessage);
                        entry.getAiChatMessages().add(aiMessage);
                    })
                    .onFailure(e -> {
                        // TODO: User-friendly error message.
                        LOGGER.error("Got an error while sending a message to AI", e);
                        aiChatComponent.setLoading(false);
                        aiChatComponent.addError(e.getMessage());
                    })
                    .executeWith(taskExecutor);
        });

        entry.getAiChatMessages().forEach(aiChatComponent::addMessage);

        setContent(aiChatComponent);
    }
}