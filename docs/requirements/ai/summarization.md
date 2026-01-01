---
parent: ai
---

# Summarization with LLMs
`feat~ai.summarization~1`

Rationale: to provide capabilities for distilling large amounts of text into concise summaries using LLMs

Needs: impl, pp

Covers: `feat~ai~1`

## General AI summarization requirements
`feat~ai.summarization.general~1`

Rationale: basic functional requirements that apply to all summarization activities regardless of the specific algorithm

Needs: impl

Covers: `feat~ai.summarization~1`

### Handle documents of any size for AI summarization
`req~ai.summarization.general.unlimited-size~1`

Rationale: users upload documents of varying lengths, from single pages to books, and the system must process them without hitting token limits

Needs: impl

Covers: `feat~ai.summarization.general~1`

## AI summarization of entries
`feat~ai.summarization.entries~1`

Rationale: specific functionality related to the summarization of database entries or document records

Needs: impl

Covers: `feat~ai.summarization~1`

### Add ability to toggle automatic AI summarization of entries
`req~ai.summarization.entries.toggle-auto~1`

Rationale: users may wish to disable automatic processing to save costs or because specific entries do not require summarization

Needs: impl

Covers: `feat~ai.summarization.entries~1`

## AI summarization algorithms
`feat~ai.summarization.algorithms~1`

Rationale: distinct strategies for processing text, necessary because different document lengths require different architectural approaches (e.g. single pass vs map-reduce)

Needs: impl

Covers: `feat~ai.summarization~1`

### "Chunked" AI summarization algorithm
`feat~ai.summarization.algorithms.chunked~1`

Rationale: a strategy for large documents that splits text into pieces, summarizes them individually, and then combines the results

Needs: impl

Reference: simplified version of the algorithm described in <https://arxiv.org/abs/2109.10862>

Covers: `feat~ai.summarization.algorithms~1`

#### Allow customization of the system prompt for chunk task in "chunked" AI summarization
`req~ai.summarization.algorithms.chunked.system-prompt-chunk~1`

Rationale: users need to adjust the underlying prompt structures to refine AI outputs

Needs: impl

Covers: `feat~ai.summarization.algorithms.chunked~1`, `feat~ai.expert-settings~1`

#### Allow customization of the user prompt for chunk task in "chunked" AI summarization
`req~ai.summarization.algorithms.chunked.user-prompt-chunk~1`

Rationale: users need to adjust the underlying prompt structures to refine AI outputs

Needs: impl

Covers: `feat~ai.summarization.algorithms.chunked~1`, `feat~ai.expert-settings~1`

#### Allow customization of the system prompt for combination task in "chunked" AI summarization
`req~ai.summarization.algorithms.chunked.system-prompt-combine~1`

Rationale: users need to adjust the underlying prompt structures to refine AI outputs

Needs: impl

Covers: `feat~ai.summarization.algorithms.chunked~1`, `feat~ai.expert-settings~1`

#### Allow customization of the user prompt for combination task in "chunked" AI summarization
`req~ai.summarization.algorithms.chunked.user-prompt-combine~1`

Rationale: users need to adjust the underlying prompt structures to refine AI outputs

Needs: impl

Covers: `feat~ai.summarization.algorithms.chunked~1`, `feat~ai.expert-settings~1`

### "Full document" AI summarization algorithm
`feat~ai.summarization.algorithms.full~1`

Rationale: a strategy for short documents that fit entirely within the LLM's context window, allowing for a single-pass summary

Needs: impl

Reference: <https://arxiv.org/abs/2307.03172>

Covers: `feat~ai.summarization.algorithms~1`

#### Allow customization of the system prompt for "full document" AI summarization
`req~ai.summarization.algorithms.full.system-prompt~1`

Rationale: users need to adjust the underlying prompt structures to refine AI outputs

Needs: impl

Covers: `feat~ai.summarization.algorithms.full~1`, `feat~ai.expert-settings~1`

#### Allow customization of the user prompt for "full document" AI summarization
`req~ai.summarization.algorithms.full.user-prompt~1`

Rationale: users need to adjust the underlying prompt structures to refine AI outputs

Needs: impl

Covers: `feat~ai.summarization.algorithms.full~1`, `feat~ai.expert-settings~1`

<!-- markdownlint-disable-file MD022 -->