package org.jabref.gui.entryeditor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.chat.AiChatComponent;
import org.jabref.gui.ai.components.chat.SingleAiChatComponent;
import org.jabref.gui.ai.components.guards.privacynotice.AiPrivacyNoticeComponent;
import org.jabref.gui.util.components.ErrorStateComponent;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

public class AiChatTab extends EntryEditorTab {
    private final BibDatabaseContext bibDatabaseContext;
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final TaskExecutor taskExecutor;

    private Optional<BibEntry> previousBibEntry = Optional.empty();

    public AiChatTab(BibDatabaseContext bibDatabaseContext,
                     AiService aiService,
                     DialogService dialogService,
                     GuiPreferences preferences,
                     TaskExecutor taskExecutor
    ) {
        this.bibDatabaseContext = bibDatabaseContext;

        this.aiService = aiService;
        this.dialogService = dialogService;

        this.aiPreferences = preferences.getAiPreferences();
        this.externalApplicationsPreferences = preferences.getExternalApplicationsPreferences();
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();

        this.taskExecutor = taskExecutor;

        setText(Localization.lang("AI chat"));
        setTooltip(new Tooltip(Localization.lang("Chat with AI about content of attached file(s)")));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    /**
     * @implNote Method similar to {@link AiSummaryTab#bindToEntry(BibEntry)}
     */
    @Override
    protected void bindToEntry(BibEntry entry) {
        previousBibEntry.ifPresent(previousBibEntry -> aiService.getChatHistoryService().closeChatHistoryForEntry(previousBibEntry));
        previousBibEntry = Optional.of(entry);

        // We omit the localization here, because it is only a chat with one entry in the {@link EntryEditor}.
        // See documentation for {@link AiChatGuardedComponent#name}.
        StringProperty chatName = new SimpleStringProperty("entry " + entry.getCitationKey().orElse("<no citation key>"));
        entry.getCiteKeyBinding().addListener((observable, oldValue, newValue) -> chatName.setValue("entry " + newValue));

        setContent(new SingleAiChatComponent(
                chatName,
                aiService.getChatHistoryService().getChatHistoryForEntry(bibDatabaseContext, entry),
                bibDatabaseContext,
                entry,
                aiService,
                dialogService,
                aiPreferences,
                externalApplicationsPreferences,
                taskExecutor
        ));
    }
}
