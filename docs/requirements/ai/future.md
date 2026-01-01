---
parent: ai
---

# Future features
`feat~ai.future~1`

Rationale: to capture upcoming enhancements and architectural refactoring for the AI system

Needs: impl

Covers: `feat~ai~1`

## Allow modification of the LLM in AI chat
`req~ai.chatting.llm-selection~1`

Rationale: users may prefer specific models for conversation based on cost, speed, or reasoning capability

Needs: impl

Covers: `feat~ai.chatting~1`

## Allow modification of the LLM in AI summary
`req~ai.summarization.llm-selection~1`

Rationale: summarization tasks may require different model strengths or token limits compared to interactive chat

Needs: impl

Covers: `feat~ai.summarization~1`

## Support editing of user messages in AI chat
`req~ai.chatting.user-message-editing~1`

Rationale: users need to correct typos or refine their queries without restarting the entire conversation context

Needs: impl

Covers: `feat~ai.chatting~1`

## Generalize the notion of the answer engine
`req~ai.chatting.answer-engine-generalization~1`

Rationale: currently answer engine is an algorithm that supplies context to LLM, however, a more powerful abstraction would be to govern how AI finds an answer. This opens up to agents

Needs: impl, dsn

Covers: `feat~ai.chatting~1`

<!-- markdownlint-disable-file MD022 -->