package org.jabref.logic.ai.rag.logic.parsing;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.util.io.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniversalFileParser implements FileParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalFileParser.class);

    private final PdfFileParser pdfFileParser = new PdfFileParser();

    public Optional<String> parse(LongTaskInfo longTaskInfo, Path path) {
        if (FileUtil.isPDFFile(path)) {
            return pdfFileParser.parse(longTaskInfo, path);
        } else {
            LOGGER.info("Unsupported file type of file: {}. Currently, only PDF files are supported", path);
            return Optional.empty();
        }
    }
}
