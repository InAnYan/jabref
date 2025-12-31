---
parent: Requirements
---
# AI

## Custom Artifact Types

- `pp`: means that this requirement should be guarded with a privacy policy banner. The feature should not do anything if the user does not accept the AI privacy policy.

## Features

### Chatting
`feat~ai-chatting~1`

#### General chat requirements
`feat~ai-chatting-general~1`
Covers: `feat~ai-chatting~1`

##### Delete messages

##### Regenerate AI responses

##### Smart prompt field

##### Clear chat history

##### See the status of the ingested files

##### See which model is currently used

##### Cancel generation of AI response

##### Show errors in chat

##### Retry generation in case of error

#### Chatting with entries
`feat~ai-chatting-entries~1`
Covers: `feat~ai-chatting~1`

##### Chat with entries UI

##### Hide the AI chat tab if a user wants

##### Store chat history

#### Chatting with groups
`feat~ai-chatting-groups~1`
Covers: `feat~ai-chatting~1`

### Summarization
`feat~ai-summarization~1`

#### Summarization of entries
`feat~ai-summarization-entries~1`
Covers: `feat~ai-summarization~1`

#### Automatic summarization of entries
`feat~ai-summarization-entries-auto~1`
Covers: `feat~ai-summarization~1`

### Citation parsing
`feat~ai-citation-parsing~1`

### Ingestion
`feat~ai-ingestion~1`

#### Handle PDF files
`feat~ai-ingestion-pdf~1`
Covers: `feat~ai-ingestion~1`

#### Ingest files when needed
`feat~ai-ingestion-lazy~1`
Covers: `feat~ai-ingestion~1`

#### Add automatic ingestion of files
`feat~ai-ingestion-auto~1`
Covers: `feat~ai-ingestion~1`

#### Clear embeddings cache
`feat~ai-ingestion-clear-cache~1`
Covers: `feat~ai-ingestion~1`

### Expert settings
`feat~ai-expert-settings~1`

#### Modify global chat inference parameters
`feat~ai-expert-settings-chat-params~1`
Covers: `feat~ai-expert-settings~1`

#### Modify global RAG parameters
`feat~ai-expert-settings-rag-params~1`
Covers: `feat~ai-expert-settings~1`

#### Modify global summarization parameters
`feat~ai-expert-settings-sum-params~1`
Covers: `feat~ai-expert-settings~1`

#### Modify AI templates
`feat~ai-expert-settings-templates~1`
Covers: `feat~ai-expert-settings~1`

#### Use different algorithms for summarization
`feat~ai-expert-settings-algo-sum~1`
Covers: `feat~ai-expert-settings~1`

#### Use different algorithms for generating AI answer
`feat~ai-expert-settings-algo-answer~1`
Covers: `feat~ai-expert-settings~1`

## Future features

### Modify LLM model per chat
`feat~ai-future-model-per-chat~1`

### Modify LLM model per summary
`feat~ai-future-model-per-summary~1`

### Edit user messages
`feat~ai-future-edit-messages~1`
