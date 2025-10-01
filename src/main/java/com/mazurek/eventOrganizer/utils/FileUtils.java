package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
public class FileUtils {

    private static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            Map.entry(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry(".odt", "application/vnd.oasis.opendocument.text"),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".avi", "video/x-msvideo")
    );

    private static final Map<String, String> TIKA_TO_STANDARD_MIME = Map.ofEntries(
            Map.entry("application/x-tika-msoffice", "application/msword"),
            Map.entry("application/x-tika-msoffice-excel", "application/vnd.ms-excel"),
            Map.entry("application/x-tika-msoffice-powerpoint", "application/vnd.ms-powerpoint"),
            // OOXML formats (Office 2007+), Tika often says "application/zip" or "application/x-tika-ooxml"
            Map.entry("application/x-tika-ooxml", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("application/zip-docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("application/zip-xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("application/zip-pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            // Sometimes Tika just says "application/zip" -> we need to resolve it ourselves
            Map.entry("application/zip", "application/vnd.openxmlformats-officedocument.wordprocessingml.document") // fallback
    );

    private final Tika tikaFileTypeDetector;

    public boolean isFileCorrect(MultipartFile uploadedFile) throws IOException {
        if (uploadedFile == null )
            return false;

        if (uploadedFile.isEmpty())
            throw new EmptyUploadedFileException();


        // Normalize case
        String originalName = Optional.ofNullable(uploadedFile.getOriginalFilename())
                .orElse("")
                .toLowerCase(Locale.ROOT)
                .trim();

        // Extract extension from filename
        Optional<String> matchedExtension = EXTENSION_TO_MIME.keySet().stream()
                .filter(originalName::endsWith)
                .findFirst();

        if (matchedExtension.isEmpty())
            return false;

        // Browser-provided MIME (still untrusted, but sometimes useful)
        String clientMime = Optional.ofNullable(uploadedFile.getContentType())
                .orElse("")
                .toLowerCase(Locale.ROOT);

        // Tika detection based on bytes + filename
        byte[] fileBytes = uploadedFile.getBytes();
        String tikaOutput = tikaFileTypeDetector.detect(fileBytes, originalName);
        String normalizedTikaMime = TIKA_TO_STANDARD_MIME.getOrDefault(
                tikaOutput.toLowerCase(Locale.ROOT),
                tikaOutput.toLowerCase(Locale.ROOT)
        );

        // Extra check for OOXML: inspect zipped content to differentiate DOCX/XLSX/PPTX
        if (normalizedTikaMime.startsWith("application/vnd.openxmlformats-officedocument")) {
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String entryName = entry.getName().toLowerCase(Locale.ROOT);
                    if (entryName.startsWith("word/")) {
                        normalizedTikaMime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                        break;
                    } else if (entryName.startsWith("xl/")) {
                        normalizedTikaMime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                        break;
                    } else if (entryName.startsWith("ppt/")) {
                        normalizedTikaMime = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                        break;
                    }
                }
            }
        }

        // Final check: extension must match MIME, and either client MIME or extension must confirm it
        String expectedMime = EXTENSION_TO_MIME.get(matchedExtension.get());
        return expectedMime.equals(normalizedTikaMime) &&
                (clientMime.equals(expectedMime) || originalName.endsWith(matchedExtension.get()));
    }


}
