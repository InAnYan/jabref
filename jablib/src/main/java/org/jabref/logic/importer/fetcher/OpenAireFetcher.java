package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fetcher for <a href="https://api.openaire.eu/graph/">OpenAIRE Graph API</a>
/// Docs: <a href="https://graph.openaire.eu/docs/apis/graph-api/searching-entities/filtering-search-results">OpenAIRE Graph API Docs</a>
@NullMarked
public class OpenAireFetcher implements SearchBasedParserFetcher {

    public static final String FETCHER_NAME = "OpenAIRE";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAireFetcher.class);

    private static final String API_URL = "https://api.openaire.eu/graph/v2/researchProducts";

    private static final int PAGE_SIZE = 10;

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);
        String searchQuery = new DefaultQueryTransformer().transformSearchQuery(queryNode).orElse("");
        if (!searchQuery.isBlank()) {
            uriBuilder.addParameter("search", searchQuery);
        }
        uriBuilder.addParameter("pageSize", String.valueOf(PAGE_SIZE));
        uriBuilder.addParameter("sortBy", "relevance DESC");
        URL result = uriBuilder.build().toURL();
        LOGGER.debug("URL for query: {}", result);
        return result;
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream);
            if (response.isEmpty()) {
                return List.of();
            }
            if (!response.has("results")) {
                return List.of();
            }
            JSONArray results = response.getJSONArray("results");
            List<BibEntry> entries = new ArrayList<>(results.length());
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                entries.add(jsonItemToBibEntry(item));
            }
            return entries;
        };
    }

    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            EntryType entryType = parseType(item.optString("type", ""));

            BibEntry entry = new BibEntry(entryType);

            // Title
            String mainTitle = item.optString("mainTitle", "");
            if (!mainTitle.isBlank()) {
                entry.withField(StandardField.TITLE, mainTitle);
            }

            // Description (abstract) - it's an array
            JSONArray descriptions = item.optJSONArray("description");
            if (descriptions != null && !descriptions.isEmpty()) {
                String abstractText = descriptions.optString(0, "");
                if (!abstractText.isBlank()) {
                    entry.withField(StandardField.ABSTRACT, abstractText);
                }
            }

            // Publication date
            String publicationDate = item.optString("publicationDate", "");
            if (!publicationDate.isBlank() && publicationDate.length() >= 4) {
                entry.withField(StandardField.YEAR, publicationDate.substring(0, 4));
            }

            // Authors
            JSONArray authors = item.optJSONArray("authors");
            if (authors != null && !authors.isEmpty()) {
                StringJoiner authorJoiner = new StringJoiner(" and ");
                for (int i = 0; i < authors.length(); i++) {
                    JSONObject author = authors.optJSONObject(i);
                    if (author != null) {
                        String fullName = author.optString("fullName", "");
                        if (!fullName.isBlank()) {
                            authorJoiner.add(fullName);
                        }
                    }
                }
                String authorString = authorJoiner.toString();
                if (!authorString.isBlank()) {
                    entry.withField(StandardField.AUTHOR, authorString);
                }
            }

            // PIDs (DOI, etc.)
            JSONArray pids = item.optJSONArray("pid");
            if (pids != null) {
                for (int i = 0; i < pids.length(); i++) {
                    JSONObject pid = pids.optJSONObject(i);
                    if (pid != null && "doi".equalsIgnoreCase(pid.optString("scheme", ""))) {
                        String doiValue = pid.optString("value", "");
                        if (!doiValue.isBlank()) {
                            entry.withField(StandardField.DOI, doiValue);
                            break;
                        }
                    }
                }
            }

            // Publisher
            String publisher = item.optString("publisher", "");
            if (!publisher.isBlank()) {
                entry.withField(StandardField.PUBLISHER, publisher);
            }

            // Journal info
            JSONObject journal = item.optJSONObject("journal");
            if (journal != null) {
                String journalName = journal.optString("name", "");
                if (!journalName.isBlank()) {
                    entry.withField(StandardField.JOURNAL, journalName);
                }
                String issn = journal.optString("issn", "");
                if (!issn.isBlank()) {
                    entry.withField(StandardField.ISSN, issn);
                }
                String volume = journal.optString("volume", "");
                if (!volume.isBlank()) {
                    entry.withField(StandardField.VOLUME, volume);
                }
                String issue = journal.optString("issue", "");
                if (!issue.isBlank()) {
                    entry.withField(StandardField.NUMBER, issue);
                }
                // OpenAIRE may use either "sp"/"ep" or "startPage"/"endPage"
                String startPage = journal.optString("sp", journal.optString("startPage", ""));
                String endPage = journal.optString("ep", journal.optString("endPage", ""));
                if (!startPage.isBlank() && !endPage.isBlank()) {
                    entry.withField(StandardField.PAGES, startPage + "--" + endPage);
                } else if (!startPage.isBlank()) {
                    entry.withField(StandardField.PAGES, startPage);
                }
            }

            // Subjects / Keywords
            JSONArray subjects = item.optJSONArray("subjects");
            if (subjects != null && !subjects.isEmpty()) {
                StringJoiner keywordJoiner = new StringJoiner(", ");
                for (int i = 0; i < subjects.length(); i++) {
                    JSONObject subjectWrapper = subjects.optJSONObject(i);
                    if (subjectWrapper != null) {
                        // OpenAIRE subjects can be nested: {"subject": {"value": "...", "scheme": "..."}}
                        JSONObject nestedSubject = subjectWrapper.optJSONObject("subject");
                        String value = nestedSubject != null
                                ? nestedSubject.optString("value", "")
                                : subjectWrapper.optString("value", "");
                        if (!value.isBlank()) {
                            keywordJoiner.add(value);
                        }
                    }
                }
                String keywords = keywordJoiner.toString();
                if (!keywords.isBlank()) {
                    entry.withField(StandardField.KEYWORDS, keywords);
                }
            }

            // URL from the OpenAIRE identifier
            String id = item.optString("id", "");
            if (!id.isBlank()) {
                entry.withField(StandardField.URL, "https://explore.openaire.eu/search/publication?articleId=" + id);
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
