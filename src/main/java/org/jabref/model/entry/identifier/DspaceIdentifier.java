package org.jabref.model.entry.identifier;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * Class for working with Dspace handles. Use with caution.
 * <p>
 * Problem is, Dspace is a software that allows to host a digital archive. Many universities run their
 * own digital archives using Dspace, and they have different URL domain. There is no definitive way
 * to determine if a {@link String} is a Dspace handle URL.
 * <p>
 * Current code uses this heuristic: URL should contain "handle/<NUMBER_1>/<NUMBER_2>". If the URL matches this pseudo-schema, then
 * JabRef will identify is as a {@link DspaceIdentifier}.
 */
public class DspaceIdentifier implements Identifier {
    private static final String REGEX = "[^/]+/handle/(\\d+)/(\\d+)";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private final URL url;

    public DspaceIdentifier(String value) {
        try {
            this.url = URI.create(value).toURL();
        } catch (MalformedURLException e) {
            // No logging of error here. It doesn't contain any valuable information, it just signals that parsing failed.
            throw new IllegalArgumentException(value + " is not a valid Dspace handle");
        }

        if (!PATTERN.matcher(this.url.getPath()).matches()) {
            throw new IllegalArgumentException(value + " is not a valid Dspace handle");
        }
    }

    public static Optional<DspaceIdentifier> parse(String value) {
        try {
            return Optional.of(new DspaceIdentifier(value));
        } catch (IllegalArgumentException e) {
            // No logging of error here. It doesn't contain any valuable information, it just signals that parsing failed.
            return Optional.empty();
        }
    }

    @Override
    public String asString() {
        return this.url.toString();
    }

    @Override
    public Field getDefaultField() {
        return StandardField.URL;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(this.url.toURI());
        } catch (URISyntaxException e) {
            // No logging of error here. It doesn't contain any valuable information, it just signals that parsing failed.
            return Optional.empty();
        }
    }

    public URL getUrl() {
        return this.url;
    }
}
