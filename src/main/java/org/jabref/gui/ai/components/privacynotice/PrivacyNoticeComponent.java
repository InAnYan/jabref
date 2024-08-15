package org.jabref.gui.ai.components.privacynotice;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.ai.AiPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivacyNoticeComponent extends ScrollPane {
    private final Logger LOGGER = LoggerFactory.getLogger(PrivacyNoticeComponent.class);

    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;

    private final Runnable onIAgreeButtonClickCallback;

    public PrivacyNoticeComponent(DialogService dialogService, AiPreferences aiPreferences, FilePreferences filePreferences, Runnable onIAgreeButtonClickCallback) {
        this.dialogService = dialogService;
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;

        this.onIAgreeButtonClickCallback = onIAgreeButtonClickCallback;

        initialize();
    }

    private void initialize() {
        WebView webView = new WebView();

        webView.getEngine().loadContent("""
                <html>
                    <body>
                        <h1>""" + Localization.lang("Privacy Notice") + """
                        </h1>

                        <p>""" + Localization.lang("JabRef uses AI providers to enable AI functionality (chatting with attached file(s) and summarization). AI provider is an external service. To enable processing of attached file(s), their contents need to be shared with the currently selected AI provider.") + """
                        </p>

                        <p>""" + Localization.lang("As soon as you ask a question, the text content of all PDFs attached to the entry are sent to external service") + """
                        </p>

                        <p>""" + Localization.lang("If you have chosen the OpenAI as AI provider, the privacy policy of OpenAI applies. You find it at %0.") + """
                    </body>
                </html>
                """);

        /*
        initPrivacyHyperlink(openAiPrivacyTextFlow, "https://openai.com/policies/privacy-policy/");
        initPrivacyHyperlink(mistralAiPrivacyTextFlow, "https://mistral.ai/terms/#privacy-policy");
        initPrivacyHyperlink(huggingFacePrivacyTextFlow, "https://huggingface.co/privacy");

        String newEmbeddingModelText = embeddingModelText.getText().replaceAll("%0", aiPreferences.getEmbeddingModel().sizeInfo());
        embeddingModelText.setText(newEmbeddingModelText);

        // Because of the https://bugs.openjdk.org/browse/JDK-8090400 bug, the text in the privacy policy cannot be
        // fully wrapped.

        embeddingModelText.wrappingWidthProperty().bind(this.widthProperty());
         */
    }

    private String makeProviderPrivacyPolicy() {

    }

    private void initPrivacyHyperlink(TextFlow textFlow, String link) {
        if (textFlow.getChildren().isEmpty() || !(textFlow.getChildren().getFirst() instanceof Text text)) {
            return;
        }

        String[] stringArray = text.getText().split("%0");

        if (stringArray.length != 2) {
            return;
        }

        text.wrappingWidthProperty().bind(this.widthProperty());
        text.setText(stringArray[0]);

        Hyperlink hyperlink = new Hyperlink(link);
        hyperlink.setWrapText(true);
        hyperlink.setFont(text.getFont());
        hyperlink.setOnAction(event -> {
            openBrowser(link);
        });

        textFlow.getChildren().add(hyperlink);

        Text postText = new Text(stringArray[1]);
        postText.setFont(text.getFont());
        postText.wrappingWidthProperty().bind(this.widthProperty());

        textFlow.getChildren().add(postText);
    }

    @FXML
    private void onIAgreeButtonClick() {
        aiPreferences.setEnableAi(true);
        onIAgreeButtonClickCallback.run();
    }

    @FXML
    private void onDjlPrivacyPolicyClick() {
        openBrowser("https://github.com/deepjavalibrary/djl/discussions/3370#discussioncomment-10233632");
    }

    private void openBrowser(String link) {
        try {
            JabRefDesktop.openBrowser(link, filePreferences);
        } catch (IOException e) {
            LOGGER.error("Error opening the browser to the Privacy Policy page of the AI provider.", e);
            dialogService.showErrorDialogAndWait(e);
        }
    }
}
