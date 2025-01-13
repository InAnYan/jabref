package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImporterTestEngine {

    private static final String TEST_RESOURCES = "src/test/resources/org/jabref/logic/importer/fileformat";

    /**
     * Get list of testing files inside {@link TEST_RESOURCES} subdirectory. This subdirectory is related to specific
     * file format, and it stores correct files. E.g.: you use this method with `subDirs` equal to `citavi` to test that
     * Citavi XML files are recognized by corresponding Citavi importer.
     * <p>
     * See also {@link getTestFilesOutOfDir}, that can be used for testing that an {@link Importer} does not recognize
     * files not of its format.
     *
     * @param subDir directory inside {@link TEST_RESOURCES}
     * @param extension extension that an {@link Importer} supports.
     * @return a {@link Stream<String>} with the names of files in the test folder
     * @throws IOException if there is a problem when trying to read the files in the file system
     */
    public static Stream<String> getTestFilesForDir(String subDir, String extension) throws IOException {
        try (Stream<Path> stream = Files.list(Path.of(TEST_RESOURCES).resolve(subDir))) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(extension))
                    .map(Path::toString);
        }
    }

    /**
     * Get list of testing files inside {@link TEST_RESOURCES} that do not belong to `subDir`. This method is used for
     * checking that an {@link Importer} does not recognize files not of its format. This function will return a stream
     * of all files in {@link TEST_RESOURCES} that do not come from `TEST_RESOURCES/{subDir}`.
     *
     * @param subDir directory to exclude
     * @param extension extension that an {@link Importer} supports.
     * @return a {@link Stream<String>} with the names of files in the test folder excluding `subdir`
     * @throws IOException if there is a problem when trying to read the files in the file system
     */
    public static Stream<String> getTestFilesOutsideOfDIr(String subDir, String extension) throws IOException {
        try (Stream<Path> stream = Files.walk(Path.of(TEST_RESOURCES))) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(extension))
                    .filter(path -> !path.getParent().getFileName().toString().equals(subDir))
                    .map(Path::toString);
        }
    }

    public static Path getTestFile(String subPath) {
        return Path.of(TEST_RESOURCES, subPath);
    }

    public static void testIsRecognizedFormat(Importer importer, String fileName) throws IOException {
        assertTrue(importer.isRecognizedFormat(getPath(fileName)));
    }

    public static void testIsNotRecognizedFormat(Importer importer, String fileName) throws IOException {
        assertFalse(importer.isRecognizedFormat(getPath(fileName)));
    }

    public static void testImportEntries(Importer importer, String fileName, String fileType) throws IOException, ImportException {
        ParserResult parserResult = importer.importDatabase(getPath(fileName));
        if (parserResult.isInvalid()) {
            throw new ImportException(parserResult.getErrorMessage());
        }
        List<BibEntry> entries = parserResult.getDatabase().getEntries();
        BibEntryAssert.assertEquals(ImporterTestEngine.class, fileName.replaceAll(fileType, ".bib"), entries);
    }

    private static Path getPath(String fileName) throws IOException {
        try {
            return Path.of(ImporterTestEngine.class.getResource(fileName).toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    public static void testImportMalformedFiles(Importer importer, String fileName) throws IOException {
        List<BibEntry> entries = importer.importDatabase(getPath(fileName)).getDatabase()
                                         .getEntries();
        assertEquals(entries, new ArrayList<BibEntry>());
    }
}
