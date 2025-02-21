package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DspaceIdentifier;
import org.jabref.model.util.OptionalUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DspaceFetcher implements IdBasedFetcher, FulltextFetcher, EntryBasedFetcher {
    public static final String NAME = "Dspace";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Optional<DspaceIdentifier> id = entry.getField(StandardField.URL).flatMap(DspaceIdentifier::parse);
        if (id.isPresent()) {
            return findFullText(id.get());
        } else {
            return Optional.empty();
        }
    }

    public Optional<URL> findFullText(DspaceIdentifier identifier) throws IOException, FetcherException {
        Document doc = Jsoup.connect(identifier.getUrl().toString()).get();

        Elements links = doc.select("a[href^=/bitstream]");

        if (!links.isEmpty()) {
            Element link = links.first();
            if (link == null) {
                return Optional.empty();
            }

            String href = link.attr("href");

            return Optional.of(new URL(identifier.getUrl(), href));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.SOURCE;
    }

    @Override
    public String getName() {
        return DspaceFetcher.NAME;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<DspaceIdentifier> id = DspaceIdentifier.parse(identifier);

        if (id.isEmpty()) {
            throw new FetcherException(Localization.lang("Invalid Dspace handle: '%0'.", identifier))
        }

        return performSearchById(id.get());
    }

    public Optional<BibEntry> performSearchById(DspaceIdentifier identifier) throws FetcherException {

    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<DspaceIdentifier> id = entry.getField(StandardField.URL).flatMap(DspaceIdentifier::parse);
        if (id.isPresent()) {
            return OptionalUtil.toList(performSearchById(id.get()));
        } else {
            return List.of();
        }
    }
}
