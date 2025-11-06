package com.petruth.personal_finance_tracker.utils;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Sanitize filenames
public class FileSecurityUtil {

    public static String sanitizeFilename(String filename) {
        // Remove path traversal attempts
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        List<String> allowed = List.of("application/pdf", "text/csv");

        if (!allowed.contains(contentType)) {
            throw new SecurityException("Invalid file type: " + contentType);
        }
    }
}