package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

public class AiChatTab extends EntryEditorTab {
    public static final String NAME = "AI chat";

    private final PreferencesService preferencesService;

    // Stores embeddings generated from full-text articles.
    // Depends on the embedding model.
    private EmbeddingStore<TextSegment> embeddingStore = null;

    // An object that augments the user prompt with relevant information from full-text articles.
    // Depends on the embedding model and the embedding store.
    private ContentRetriever contentRetriever = null;

    // Holds and performs the conversation with user. Stores the message history and manages API calls.
    // Depends on the chat language model and content retriever.
    private ConversationalRetrievalChain chain = null;

    /*
        Classes from langchain:
        - Global:
            - EmbeddingsModel - put into preferences.
        - Per entry:
            - EmbeddingsStore - stores embeddings of full-text article.
        - Per chat:
            - ContentRetriever - a thing that augments the user prompt with relevant information.
            - ConversationalRetrievalChain - main wrapper between the user and AI. Chat history, API calls.

        - Per situation:
            - EmbeddingsIngestor - ingests embeddings of full-text article (an algorithm part,
                                   we don't need to store it somewhere).
     */

    public AiChatTab(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;

        setText(Localization.lang(NAME));
        setTooltip(new Tooltip(Localization.lang("AI chat with full-text article")));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return true;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (entry.getFiles().isEmpty()) {
            setContent(new Label(Localization.lang("No files attached")));
        } else if (!entry.getFiles().stream().allMatch(file -> file.getFileType().equals("PDF"))) {
            /*
                QUESTION: What is the type of file.getFileType()????
                I thought it is the part after the dot, but it turns out not.
                I got the "PDF" string by looking at tests.
             */
            setContent(new Label(Localization.lang("Only PDF files are supported")));
        } else {
            try {
                bindToEntryRaw(entry);
            } catch (IOException e) {
                setContent(new Label(e.getMessage()));
            }
        }
    }

    /*
        An idea how to implement this:
        what if we just start building this content, and then when the method sees "there is no files" it will raise an exception.
        or it will raise an exception when it does not support a filetype?
        And then the exception is caught in "bindToEntry" and a label of error is given to user.
     */
    private void bindToEntryRaw(BibEntry entry) throws IOException {
        configureAI(entry);
        makeContent();
    }

    private void makeContent() {
        Label askLabel = new Label(Localization.lang("Ask AI") + ": ");

        TextField promptField = new TextField();

        Button submitButton = new Button(Localization.lang("Submit"));

        HBox promptBox = new HBox(askLabel, promptField, submitButton);

        Label answerLabel = new Label(Localization.lang("Answer") + ": ");

        Label realAnswerLabel = new Label();

        HBox answerBox = new HBox(answerLabel, realAnswerLabel);

        VBox vbox = new VBox(promptBox, answerBox);

        submitButton.setOnAction(e -> {
            // TODO: Check if the prompt is empty.
            realAnswerLabel.setText(chain.execute(promptField.getText()));
        });

        setContent(vbox);
    }

    private void configureAI(BibEntry entry) throws IOException {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                                              .embeddingStore(this.embeddingStore)
                                              .embeddingModel(preferencesService.getAiPreferences().getEmbeddingModel())
                                              .build();

        for (LinkedFile linkedFile : entry.getFiles()) {
            String fileContents = Files.readString(Path.of(linkedFile.getLink()));
            Document document = new Document(fileContents);
            ingestor.ingest(document);
        }

        this.contentRetriever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(this.embeddingStore)
                .embeddingModel(preferencesService.getAiPreferences().getEmbeddingModel())
                .build();

        this.chain = ConversationalRetrievalChain
                .builder()
                .chatLanguageModel(preferencesService.getAiPreferences().getChatModel())
                .contentRetriever(this.contentRetriever)
                .build();
    }
}