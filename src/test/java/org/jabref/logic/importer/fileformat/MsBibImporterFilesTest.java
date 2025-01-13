package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.layout.format.FileLink;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MsBibImporterFilesTest {

    private static final String FILE_ENDING = ".xml";

    private static Stream<String> fileNames() throws IOException {
        return ImporterTestEngine.getTestFilesForDir("msBib", FILE_ENDING);
    }

    private static Stream<String> invalidFileNames() throws IOException {
        return ImporterTestEngine.getTestFilesOutsideOfDIr("msBib", FILE_ENDING);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void isRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new MsBibImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void isNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(new MsBibImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void importEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(new MsBibImporter(), fileName, FILE_ENDING);
    }
}
