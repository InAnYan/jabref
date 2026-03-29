package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
class OpenAireFetcherTest {

    private OpenAireFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new OpenAireFetcher();
    }

    @Test
    void getName() {
        assertEquals("OpenAIRE", fetcher.getName());
    }

    @Test
    void getURLForQueryBuildsSearchUrl() throws MalformedURLException, URISyntaxException {
        String query = "OpenAIRE Graph";
        SearchQuery searchQueryObject = new SearchQuery(query);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
        URL url = fetcher.getURLForQuery(visitor.visitStart(searchQueryObject.getContext()));
        assertTrue(url.toString().startsWith("https://api.openaire.eu/graph/v2/researchProducts"));
        assertTrue(url.toString().contains("search="));
        assertTrue(url.toString().contains("pageSize=10"));
    }

    @Test
    void searchByQueryReturnsResults() throws FetcherException {
        List<BibEntry> result = fetcher.performSearch("OpenAIRE Graph");
        assertFalse(result.isEmpty());
    }

    @Test
    void searchByEmptyQueryReturnsEmptyList() throws FetcherException {
        List<BibEntry> result = fetcher.performSearch("");
        assertEquals(List.of(), result);
    }

    @Test
    void searchResultHasTitleField() throws FetcherException {
        List<BibEntry> result = fetcher.performSearch("knowledge graphs");
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(entry -> entry.getField(StandardField.TITLE).isPresent()));
    }
}
