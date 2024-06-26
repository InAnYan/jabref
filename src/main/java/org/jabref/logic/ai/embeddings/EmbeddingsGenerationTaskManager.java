package org.jabref.logic.ai.embeddings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

/**
 * This class manages a queue of embedding generation tasks for one {@link BibEntry} in a {@link org.jabref.model.database.BibDatabase}.
 * <p>
 * {@link org.jabref.model.database.BibDatabase} will listen for entries updates and add them to the queue. This class
 * will start a background task for generating the embeddings for each entry in the queue.
 * <p>
 * This class also provides an ability to prioritize the entry in the queue. But it seems not to work well.
 */
public class EmbeddingsGenerationTaskManager extends BackgroundTask<Void> {
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final AiService aiService;
    private final TaskExecutor taskExecutor;

    private final AiIngestor aiIngestor;

    private final List<LinkedFile> linkedFileQueue = new ArrayList<>();
    private int numOfProcessedFiles = 0;

    private final Object lock = new Object();
    private boolean isRunning = false;
    private boolean isBlockingNewTasks = false;

    public EmbeddingsGenerationTaskManager(BibDatabaseContext databaseContext, FilePreferences filePreferences, AiService aiService, TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.aiService = aiService;
        this.taskExecutor = taskExecutor;

        this.aiIngestor = new AiIngestor(aiService);

        configure();

        listenToPreferences();
    }

    private void configure() {
        showToUser(true);
        willBeRecoveredAutomatically(true);
        updateProgress(1, 1);
        titleProperty().set(Localization.lang("Embeddings generation"));

        this.onFailure(e -> {
            throw new RuntimeException(e);
        });
    }

    private void listenToPreferences() {
        // TODO: Will these listeners align with AiIngestor preference listeners?
        aiService.getPreferences().onEmbeddingsParametersChange(this::invalidate);
    }

    public void addToProcess(Collection<BibEntry> bibEntries) {
        bibEntries.forEach(this::addToProcess);
    }

    public void removeFromProcess(Collection<BibEntry> bibEntries) {
        bibEntries.forEach(this::removeFromProcess);
    }

    public void addToProcess(BibEntry bibEntry) {
        addToProcess(bibEntry.getFiles());
    }

    public void removeFromProcess(BibEntry bibEntry) {
        removeFromProcess(bibEntry.getFiles());
    }

    public void addToProcess(List<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::addToProcess);
    }

    public void removeFromProcess(List<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::removeFromProcess);
    }

    public void addToProcess(LinkedFile linkedFile) {
        if (!isBlockingNewTasks) {
            linkedFileQueue.add(linkedFile);

            synchronized (lock) {
                if (!isRunning) {
                    this.executeWith(taskExecutor);
                    showToUser(false);
                }
            }
        }
    }

    public AutoCloseable blockNewTasks() {
        synchronized (lock) {
            isBlockingNewTasks = true;
        }

        return () -> {
            synchronized (lock) {
                isBlockingNewTasks = false;
            }
        };
    }

    public void removeFromProcess(LinkedFile linkedFile) {
        aiService.getEmbeddingsManager().removeIngestedFile(linkedFile.getLink());
    }

    public void removeFromProcess(Set<String> linksToRemove) {
        linksToRemove.forEach(this::removeFromProcess);
    }

    public void removeFromProcess(String link) {
        aiService.getEmbeddingsManager().removeIngestedFile(link);
    }

    public void updateEmbeddings(BibDatabaseContext bibDatabaseContext) {
        Set<String> linksToRemove = aiService.getEmbeddingsManager().getIngestedFilesTracker().getListOfIngestedFilesLinks();
        bibDatabaseContext.getEntries().stream()
                       .flatMap(entry -> entry.getFiles().stream())
                       .map(LinkedFile::getLink)
                       .forEach(linksToRemove::remove);

        removeFromProcess(linksToRemove);

        addToProcess(bibDatabaseContext.getEntries());
    }

    @Override
    protected Void call() throws Exception {
        synchronized (lock) {
            isRunning = true;
        }

        updateProgress();

        while (!linkedFileQueue.isEmpty() && !isCanceled()) {
            LinkedFile linkedFile = linkedFileQueue.removeLast();

            ingestLinkedFile(linkedFile);

            ++numOfProcessedFiles;
            updateProgress();
        }

        synchronized (lock) {
            isRunning = false;
        }

        return null;
    }

    private void ingestLinkedFile(LinkedFile linkedFile) {
        aiIngestor.ingestLinkedFile(linkedFile, databaseContext, filePreferences);
    }

    private void updateProgress() {
        updateMessage(Localization.lang("Generated embeddings %0 of %1 linked files", numOfProcessedFiles, numOfProcessedFiles + linkedFileQueue.size()));
        updateProgress(numOfProcessedFiles, numOfProcessedFiles + linkedFileQueue.size());
    }

    public void invalidate() {
        // TODO: Is this method right?
        // 1. How to stop if running.
        // 2. Is it okay to clear queue?
        // 3. Is it okay to 1) remove, 2) add to queue everything?

        linkedFileQueue.clear();
        databaseContext.getEntries().forEach(entry -> {
            removeFromProcess(entry);
            addToProcess(entry);
        });
    }

    public void moveToFront(Collection<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::moveToFront);
    }

    /**
     * This method is responsible for prioritizing a {@link LinkedFile} in the queue.
     * <p>
     * Queue is thought like a stack, so we just push the {@link LinkedFile} to the top of the stack.
     * <p>
     * It is not an error to add a {@link LinkedFile} if it is already in the queue or in process. In that case, it will be ignored by {@link AiIngestor}.
     */
    public void moveToFront(LinkedFile linkedFile) {
        linkedFileQueue.add(linkedFile);
    }
}
