package org.jabref.logic.ai.ingestion.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHasherTest {

    @Test
    void testComputeHashProducesConsistentHash(@TempDir Path tempDir) throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content", StandardOpenOption.CREATE);

        // Compute hash twice
        String hash1 = FileHasher.computeHash(testFile);
        String hash2 = FileHasher.computeHash(testFile);

        // Hashes should be identical
        assertEquals(hash1, hash2, "Same file should produce the same hash");
    }

    @Test
    void testComputeHashDifferentForDifferentContent(@TempDir Path tempDir) throws IOException {
        // Create two test files with different content
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "Content 1", StandardOpenOption.CREATE);
        Files.writeString(file2, "Content 2", StandardOpenOption.CREATE);

        // Compute hashes
        String hash1 = FileHasher.computeHash(file1);
        String hash2 = FileHasher.computeHash(file2);

        // Hashes should be different
        assertNotEquals(hash1, hash2, "Different files should produce different hashes");
    }

    @Test
    void testComputeHashThrowsIOExceptionForNonExistentFile() {
        Path nonExistentFile = Path.of("/non/existent/file.txt");

        assertThrows(IOException.class, () -> FileHasher.computeHash(nonExistentFile),
                "Should throw IOException for non-existent file");
    }

    @Test
    void testComputeHashProducesHexString(@TempDir Path tempDir) throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test", StandardOpenOption.CREATE);

        // Compute hash
        String hash = FileHasher.computeHash(testFile);

        // Hash should be 64 characters (256 bits / 4 bits per hex character)
        assertEquals(64, hash.length(), "SHA-256 hash should be 64 hex characters");

        // All characters should be hexadecimal (0-9, a-f)
        assertTrue(hash.matches("[0-9a-f]+"), "Hash should contain only hexadecimal characters");
    }
}

