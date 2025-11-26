package org.jabref.logic.ai.rag.logic.parsing;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.util.io.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniversalFileParser implements FileParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalFileParser.class);

    private final PdfFileParser pdfFileParser = new PdfFileParser();

    public Optional<String> parse(Path path, ReadOnlyBooleanProperty shutdownSignal) {
        if (FileUtil.isPDFFile(path)) {
            return pdfFileParser.parse(path, shutdownSignal);
        } else {
            LOGGER.info("Unsupported file type of file: {}. Currently, only PDF files are supported", path);
            return Optional.empty();
        }
    }
}
