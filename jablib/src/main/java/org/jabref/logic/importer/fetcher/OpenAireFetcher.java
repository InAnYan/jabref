package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fetcher for the <a href="https://api.openaire.eu/graph/">OpenAIRE Graph API</a>
///
/// @see <a href="https://graph.openaire.eu/docs/apis/graph-api/searching-entities/filtering-search-results">API documentation</a>
public class OpenAireFetcher implements PagedSearchBasedParserFetcher {

    public static final String FETCHER_NAME = "OpenAIRE";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAireFetcher.class);

    private static final String API_URL = "https://api.openaire.eu/graph/v2/researchProducts";

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode, int pageNumber) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);
        new DefaultQueryTransformer().transformSearchQuery(queryNode).ifPresent(
                query -> uriBuilder.addParameter("search", query));
        uriBuilder.addParameter("page", String.valueOf(pageNumber + 1));
        uriBuilder.addParameter("pageSize", String.valueOf(getPageSize()));
        uriBuilder.addParameter("sortBy", "relevance DESC");
        URL result = uriBuilder.build().toURL();
        LOGGER.debug("URL for query: {}", result);
        return result;
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream);
            if (response.isEmpty() || !response.has("results")) {
                return List.of();
            }
            JSONArray results = response.getJSONArray("results");
            List<BibEntry> entries = new ArrayList<>(results.length());
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                entries.add(parseJSONtoBibEntry(item));
            }
            return entries;
        };
    }

    private BibEntry parseJSONtoBibEntry(JSONObject item) throws ParseException {
        try {
            BibEntry entry = new BibEntry(parseType(item.optString("type", "")));

            entry.setField(StandardField.TITLE, item.optString("mainTitle", ""));

            // Description (abstract) - provided as an array
            JSONArray descriptions = item.optJSONArray("description");
            if (descriptions != null && !descriptions.isEmpty()) {
                entry.setField(StandardField.ABSTRACT, descriptions.optString(0, ""));
            }

            // Publication date - extract the year portion
            String publicationDate = item.optString("publicationDate", "");
            if (publicationDate.length() >= 4) {
                entry.setField(StandardField.YEAR, publicationDate.substring(0, 4));
            }

            // Authors
            JSONArray authors = item.optJSONArray("authors");
            if (authors != null && !authors.isEmpty()) {
                List<String> authorNames = new ArrayList<>(authors.length());
                for (int i = 0; i < authors.length(); i++) {
                    JSONObject author = authors.optJSONObject(i);
                    if (author != null) {
                        String fullName = author.optString("fullName", "");
                        if (!fullName.isBlank()) {
                            authorNames.add(fullName);
                        }
                    }
                }
                if (!authorNames.isEmpty()) {
                    entry.setField(StandardField.AUTHOR,
                            AuthorList.parse(String.join(" and ", authorNames)).getAsLastFirstNamesWithAnd(false));
                }
            }

            // PIDs - extract DOI if present
            JSONArray pids = item.optJSONArray("pid");
            if (pids != null) {
                for (int i = 0; i < pids.length(); i++) {
                    JSONObject pid = pids.optJSONObject(i);
                    if (pid != null && "doi".equalsIgnoreCase(pid.optString("scheme", ""))) {
                        entry.setField(StandardField.DOI, pid.optString("value", ""));
                        break;
                    }
                }
            }

            entry.setField(StandardField.PUBLISHER, item.optString("publisher", ""));

            // Journal info
            JSONObject journal = item.optJSONObject("journal");
            if (journal != null) {
                entry.setField(StandardField.JOURNAL, journal.optString("name", ""));
                entry.setField(StandardField.ISSN, journal.optString("issn", ""));
                entry.setField(StandardField.VOLUME, journal.optString("volume", ""));
                entry.setField(StandardField.NUMBER, journal.optString("issue", ""));
                // OpenAIRE uses "sp"/"ep" for start/end page
                String startPage = journal.optString("sp", "");
                String endPage = journal.optString("ep", "");
                if (!startPage.isBlank() && !endPage.isBlank()) {
                    entry.setField(StandardField.PAGES, startPage + "--" + endPage);
                } else if (!startPage.isBlank()) {
                    entry.setField(StandardField.PAGES, startPage);
                }
            }

            // Subjects as keywords
            JSONArray subjects = item.optJSONArray("subjects");
            if (subjects != null && !subjects.isEmpty()) {
                List<String> keywordList = new ArrayList<>(subjects.length());
                for (int i = 0; i < subjects.length(); i++) {
                    JSONObject subjectWrapper = subjects.optJSONObject(i);
                    if (subjectWrapper != null) {
                        // Subject value may be nested under a "subject" key
                        JSONObject nestedSubject = subjectWrapper.optJSONObject("subject");
                        String value = nestedSubject != null
                                ? nestedSubject.optString("value", "")
                                : subjectWrapper.optString("value", "");
                        if (!value.isBlank()) {
                            keywordList.add(value);
                        }
                    }
                }
                if (!keywordList.isEmpty()) {
                    entry.setField(StandardField.KEYWORDS, String.join(", ", keywordList));
                }
            }

            // Link to the OpenAIRE explore page using the entry's OpenAIRE id
            String id = item.optString("id", "");
            if (!id.isBlank()) {
                entry.setField(StandardField.URL, "https://explore.openaire.eu/search/publication?articleId=" + id);
            }

            return entry;
        } catch (JSONException e) {
            throw new ParseException("Could not parse OpenAIRE response", e);
        }
    }

    private EntryType parseType(String type) {
        return switch (type.toLowerCase()) {
            case "dataset" -> StandardEntryType.Dataset;
            case "software" -> StandardEntryType.Software;
            case "other" -> StandardEntryType.Misc;
            default -> StandardEntryType.Article;
        };
    }
}
