package org.jabref.logic.ai.rag.logic.parsing;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.pdf.InterruptablePDFTextStripper;
import org.jabref.logic.xmp.XmpUtilReader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfFileParser implements FileParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfFileParser.class);

    @Override
    public Optional<String> parse(Path path, ReadOnlyBooleanProperty shutdownSignal) {
        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(path)) {
            int lastPage = document.getNumberOfPages();
            StringWriter writer = new StringWriter();

            InterruptablePDFTextStripper stripper = new InterruptablePDFTextStripper(shutdownSignal);
            stripper.setStartPage(1);
            stripper.setEndPage(lastPage);
            stripper.writeText(document, writer);

            if (shutdownSignal.get()) {
                return Optional.empty();
            }

            return Optional.of(writer.toString());
        } catch (IOException e) {
            LOGGER.error("An error occurred while reading the PDF file: {}", path, e);
            return Optional.empty();
        }
    }
}
