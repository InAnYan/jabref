---
parent: ai
---

# Citation parsing with LLMs
`feat~ai.citation-parsing~1`

Rationale: to enable the automatic extraction and identification of references within text using AI capabilities

Needs: pp

Needs: impl

Covers: `feat~ai~1`

## Support parsing of citations using LLMs
`req~ai.citation-parsing.llm-execution~1`

Rationale: utilizing LLMs allows for flexible parsing of unstructured text that traditional regex cannot handle effectively

Needs: impl

Covers: `feat~ai.citation-parsing~1`

## Allow customization of the system prompt for LLM citation parsing
`req~ai.citation-parsing.system-prompt-config~1`

Rationale: different citation styles or strictness levels require adjusting the baseline instructions (system prompt) given to the AI

Needs: impl

Covers: `feat~ai.citation-parsing~1`, `feat~ai.expert-settings~1`

## Allow customization of the user prompt for LLM citation parsing
`req~ai.citation-parsing.user-prompt-config~1`

Rationale: different citation styles or strictness levels require adjusting the baseline instructions (system prompt) given to the AI

Needs: impl

Covers: `feat~ai.citation-parsing~1`, `feat~ai.expert-settings~1`

<!-- markdownlint-disable-file MD022 -->
