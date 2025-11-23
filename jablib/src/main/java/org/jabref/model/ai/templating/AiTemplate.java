package org.jabref.model.ai.templating;

public enum AiTemplate {
    // Templates that are used in AI chats.
    CHATTING_SYSTEM_MESSAGE,
    CHATTING_USER_MESSAGE,

    // Templates that are used for summarization of text chunks.
    SUMMARIZATION_CHUNK_SYSTEM_MESSAGE,
    SUMMARIZATION_CHUNK_USER_MESSAGE,

    // Templates that are used for combining summaries of several chunks.
    SUMMARIZATION_COMBINE_SYSTEM_MESSAGE,
    SUMMARIZATION_COMBINE_USER_MESSAGE,

    // Templates that are used to convert a raw citation into a {@link BibEntry}.
    CITATION_PARSING_SYSTEM_MESSAGE,
    CITATION_PARSING_USER_MESSAGE;
}
