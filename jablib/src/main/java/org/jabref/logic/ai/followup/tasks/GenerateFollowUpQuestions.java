package org.jabref.logic.ai.followup.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;

import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateFollowUpQuestions extends BackgroundTask<List<String>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateFollowUpQuestions.class);
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\s*\\d+\\.\\s*(.+)$", Pattern.MULTILINE);
    private static final String QUOTE_REMOVAL_PATTERN = "^[\"']|[\"']$";

    private final ChatModel chatModel;
    private final AiPreferences aiPreferences;
    private final String userMessage;
    private final String aiResponse;

    public GenerateFollowUpQuestions(
            ChatModel chatModel,
            AiPreferences aiPreferences,
            String userMessage,
            String aiResponse
    ) {
        this.chatModel = chatModel;
        this.aiPreferences = aiPreferences;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;

        titleProperty().set(Localization.lang("Generating follow-up questions..."));
    }

    @Override
    public List<String> call() throws Exception {
        String prompt = AiTemplateRenderer.renderFollowUpQuestionsPrompt(
                aiPreferences.getFollowUpQuestionsTemplate(),
                userMessage,
                aiResponse,
                aiPreferences.getFollowUpQuestionsCount()
        );

        LOGGER.debug("Generating follow-up questions for conversation");

        String responseText = chatModel.chat(List.of(new UserMessage(prompt))).aiMessage().text();

        LOGGER.debug("Received follow-up questions response: {}", responseText);

        List<String> questions = parseQuestions(responseText);

        LOGGER.debug("Generated {} follow-up questions", questions.size());

        return questions;
    }

    private List<String> parseQuestions(String response) {
        List<String> questions = new ArrayList<>();

        Matcher matcher = NUMBERED_PATTERN.matcher(response);

        while (matcher.find() && questions.size() < aiPreferences.getFollowUpQuestionsCount()) {
            String question = matcher.group(1).trim();

            question = question.replaceAll(QUOTE_REMOVAL_PATTERN, "");

            if (isValidQuestion(question)) {
                questions.add(question);
            }
        }

        if (questions.isEmpty()) {
            LOGGER.debug("Numbered format parsing failed, trying line-by-line parsing");
            String[] lines = response.split("\n");

            for (String line : lines) {
                if (questions.size() >= aiPreferences.getFollowUpQuestionsCount()) {
                    break;
                }

                line = line.trim()
                           .replaceAll("^[-*•]\\s*", "")
                           .replaceAll("^\\d+\\.\\s*", "")
                           .replaceAll(QUOTE_REMOVAL_PATTERN, "");

                if (isValidQuestion(line)) {
                    questions.add(line);
                }
            }
        }

        return questions;
    }

    private boolean isValidQuestion(String question) {
        return question != null && !question.isBlank();
    }
}
