package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MedlineImporterFilesTest {

    private static final String FILE_ENDING = ".xml";

    private static Stream<String> fileNames() throws IOException {
        return ImporterTestEngine.getTestFilesForDir("medline", FILE_ENDING);
    }

    private static Stream<String> invalidFileNames() throws IOException {
        return ImporterTestEngine.getTestFilesOutsideOfDIr("medline", FILE_ENDING);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void isRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new MedlineImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void isNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(new MedlineImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void importEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(new MedlineImporter(), fileName, FILE_ENDING);
    }

    private static Stream<String> malformedFileNames() throws IOException {
        return Stream.of(ImporterTestEngine.getTestFile("medline/MedlineImporterTestMalformedEntry.xml"));
    }

    @ParameterizedTest
    @MethodSource("malformedFileNames")
    void importMalfomedFiles(String fileName) throws IOException {
        ImporterTestEngine.testImportMalformedFiles(new MedlineImporter(), fileName);
    }
}
