package org.jabref.logic.ai.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHasher {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileHasher.class);
    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    /**
     * Computes the SHA-256 hash of a file.
     *
     * @param path the path to the file
     * @return the hex-encoded hash of the file
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static String computeHash(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            try (var inputStream = Files.newInputStream(path)) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array
     * @return the hex-encoded string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
