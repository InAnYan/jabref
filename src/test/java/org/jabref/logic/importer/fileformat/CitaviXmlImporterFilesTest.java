package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CitaviXmlImporterFilesTest {

    private static final String FILE_ENDING = ".ctv6bak";
    private final CitaviXmlImporter citaviXmlImporter = new CitaviXmlImporter();

    private static Stream<String> fileNames() throws IOException {
        return ImporterTestEngine.getTestFilesForDir("citaviXml", FILE_ENDING);
    }

    private static Stream<String> invalidFileNames() throws IOException {
        return ImporterTestEngine.getTestFilesOutsideOfDIr("citaviXml", FILE_ENDING);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void isRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(citaviXmlImporter, fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void isNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(citaviXmlImporter, fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void importEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(citaviXmlImporter, fileName, FILE_ENDING);
    }
}
