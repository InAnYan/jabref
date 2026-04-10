package org.jabref.logic.ai.followup.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.templates.AiTemplateRenderer;

import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Generates follow-up questions based on a completed user/AI conversation turn.
public class GenerateFollowUpQuestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateFollowUpQuestions.class);
    private static final int MIN_QUESTION_LENGTH = 5;
    private static final int MAX_QUESTION_LENGTH = 100;
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\s*\\d+\\.\\s*(.+)$", Pattern.MULTILINE);

    private final ChatModel chatModel;
    private final AiPreferences aiPreferences;

    public GenerateFollowUpQuestions(ChatModel chatModel, AiPreferences aiPreferences) {
        this.chatModel = chatModel;
        this.aiPreferences = aiPreferences;
    }

    public List<String> generate(String userMessage, String aiResponse) {
        try {
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
        } catch (Exception e) {
            LOGGER.warn("Failed to generate follow-up questions", e);
            return new ArrayList<>();
        }
    }

    private List<String> parseQuestions(String response) {
        List<String> questions = new ArrayList<>();

        Matcher matcher = NUMBERED_PATTERN.matcher(response);

        while (matcher.find() && questions.size() < aiPreferences.getFollowUpQuestionsCount()) {
            String question = matcher.group(1).trim();

            question = question.replaceAll("^[\"']|[\"']$", "");

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
                           .replaceAll("^[\"']|[\"']$", "");

                if (isValidQuestion(line)) {
                    questions.add(line);
                }
            }
        }

        return questions;
    }

    private boolean isValidQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }

        int length = question.length();
        if (length < MIN_QUESTION_LENGTH || length > MAX_QUESTION_LENGTH) {
            LOGGER.debug("Question length {} is outside valid range [{}, {}]", length, MIN_QUESTION_LENGTH, MAX_QUESTION_LENGTH);
            return false;
        }

        return true;
    }
}
