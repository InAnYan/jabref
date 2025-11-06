package org.jabref.logic.ai.framework.llms;

import java.util.List;

import org.jabref.logic.ai.framework.messages.ChatMessage;
import org.jabref.logic.ai.framework.messages.LlmMessage;

/**
 * Interface for Large Language Model implementations.
 */
public interface LlmModel {

    /**
     * Sends a chat conversation to the LLM and returns the generated response.
     *
     * @param messages the conversation messages in chronological order
     * @param parameters inference parameters controlling the generation
     * @return the LLM's response message
     * @throws LlmInferenceException if the inference fails
     */
    LlmMessage chat(List<ChatMessage> messages, LlmInferenceParameters parameters) throws LlmInferenceException;
}
