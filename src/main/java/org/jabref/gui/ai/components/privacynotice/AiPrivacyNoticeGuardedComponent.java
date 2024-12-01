package org.jabref.gui.ai.components.privacynotice;

import java.util.concurrent.Callable;
import java.util.function.Function;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.AiChatGuardedComponent;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.DynamicallyChangeableNode;
import org.jabref.logic.ai.AiPreferences;

/**
 * A class that guards a component, before AI privacy policy is accepted.
 * Remember to call rebuildUi() method after initializing the guarded component. See {@link AiChatGuardedComponent} to look how it works.
 */
public abstract class AiPrivacyNoticeGuardedComponent extends DynamicallyChangeableNode {
    private final AiPreferences aiPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final DialogService dialogService;

    public AiPrivacyNoticeGuardedComponent(AiPreferences aiPreferences, ExternalApplicationsPreferences externalApplicationsPreferences, DialogService dialogService) {
        this.aiPreferences = aiPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.dialogService = dialogService;

        aiPreferences.enableAiProperty().addListener(observable -> rebuildUi());
    }

    public final void rebuildUi() {
        setContent((_) -> showPrivacyPolicyGuardedContent());
    }

    protected abstract Node showPrivacyPolicyGuardedContent();

    protected void setContent(Function<Void, Node> content) {
        if (aiPreferences.getEnableAi()) {
            super.setContent(content.apply(null));
        } else {
            super.setContent(
                    new PrivacyNoticeComponent(
                            aiPreferences,
                            this::rebuildUi,
                            externalApplicationsPreferences,
                            dialogService
                    )
            );
        }
    }
}
