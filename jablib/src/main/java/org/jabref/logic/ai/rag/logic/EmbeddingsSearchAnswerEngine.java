package org.jabref.logic.ai.rag.logic;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner;
import org.jabref.model.ai.identifiers.BibEntryAiIdentifier;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.ListUtil;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddingsSearchAnswerEngine implements AnswerEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingsSearchAnswerEngine.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final double minimumScore;
    private final int maximumResultsCount;

    public EmbeddingsSearchAnswerEngine(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            double minimumScore,
            int maximumResultsCount
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.minimumScore = minimumScore;
        this.maximumResultsCount = maximumResultsCount;
    }

    @Override
    public List<RelevantInformation> process(
            String query,
            List<BibEntryAiIdentifier> entriesFilter
    ) {
        // TODO: Simplify.

        List<BibEntry> entries = entriesFilter
                .stream()
                .map(BibEntryAiIdentifier::entry)
                .toList();

        List<LinkedFile> linkedFiles = ListUtil.getLinkedFiles(entries).toList();

        Optional<Filter> filter;
        if (linkedFiles.isEmpty()) {
            filter = Optional.empty();
        } else {
            filter = Optional.of(MetadataFilterBuilder
                    .metadataKey(EmbeddingsCleaner.LINK_METADATA_KEY)
                    .isIn(linkedFiles
                            .stream()
                            .map(LinkedFile::getLink)
                            .toList()
                    ));
        }

        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest
                .builder()
                .maxResults(maximumResultsCount)
                .minScore(minimumScore)
                .filter(filter.orElse(null))
                .queryEmbedding(embeddingModel.embed(query).content())
                .build();

        EmbeddingSearchResult<TextSegment> embeddingSearchResult = embeddingStore.search(embeddingSearchRequest);

        List<RelevantInformation> excerpts = embeddingSearchResult
                .matches()
                .stream()
                .map(EmbeddingMatch::embedded)
                .map(textSegment -> {
                    String link = textSegment.metadata().getString(EmbeddingsCleaner.LINK_METADATA_KEY);

                    if (link == null) {
                        return new RelevantInformation(null, textSegment.text());
                    } else {
                        return new RelevantInformation(
                                BibEntryAiIdentifier.findEntryByLink(entriesFilter, link).flatMap(BibEntry::getCitationKey).orElse(null),
                                textSegment.text()
                        );
                    }
                })
                .toList();

        LOGGER.debug("Found excerpts for the message: {}", excerpts);

        return excerpts;
    }

    @Override
    public AnswerEngineKind getKind() {
        return AnswerEngineKind.EMBEDDINGS_SEARCH;
    }
}
