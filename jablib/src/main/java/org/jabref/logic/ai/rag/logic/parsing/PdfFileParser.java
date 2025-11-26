package org.jabref.logic.ai.rag.logic.parsing;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.ai.util.LongTaskInfo;
import org.jabref.logic.pdf.InterruptablePDFTextStripper;
import org.jabref.logic.xmp.XmpUtilReader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfFileParser implements FileParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfFileParser.class);

    @Override
    public Optional<String> parse(LongTaskInfo longTaskInfo, Path path) {
        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(path)) {
            int lastPage = document.getNumberOfPages();
            StringWriter writer = new StringWriter();

            InterruptablePDFTextStripper stripper = new InterruptablePDFTextStripper(longTaskInfo.shutdownSignal());
            stripper.setStartPage(1);
            stripper.setEndPage(lastPage);
            stripper.writeText(document, writer);

            if (longTaskInfo.shutdownSignal().get()) {
                // TODO: Why not throw interrupted exception?
                return Optional.empty();
            }

            return Optional.of(writer.toString());
        } catch (IOException e) {
            LOGGER.error("An error occurred while reading the PDF file: {}", path, e);
            return Optional.empty();
        }
    }
}
