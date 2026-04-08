package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
public class FileUtils {

    private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PPTX_MIME = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ODT_MIME = "application/vnd.oasis.opendocument.text";

    private static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", DOCX_MIME),
            Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            Map.entry(".pptx", PPTX_MIME),
            Map.entry(".odt", ODT_MIME),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", XLSX_MIME),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".avi", "video/x-msvideo")
    );

    private static final Map<String, String> TIKA_TO_STANDARD_MIME = Map.ofEntries(
            Map.entry("application/x-tika-msoffice", "application/msword"),
            Map.entry("application/x-tika-msoffice-excel", "application/vnd.ms-excel"),
            Map.entry("application/x-tika-msoffice-powerpoint", "application/vnd.ms-powerpoint"),
            Map.entry("application/zip-docx", DOCX_MIME),
            Map.entry("application/zip-xlsx", XLSX_MIME),
            Map.entry("application/zip-pptx", PPTX_MIME)
    );

    private static final Set<String> ZIP_CONTAINER_MIME_TYPES = Set.of(
            "application/zip",
            "application/x-tika-ooxml"
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
        String tikaOutput = tikaFileTypeDetector.detect(fileBytes, originalName).toLowerCase(Locale.ROOT);
        String normalizedTikaMime = ZIP_CONTAINER_MIME_TYPES.contains(tikaOutput)
                ? detectZipContainerMime(fileBytes).orElse(tikaOutput)
                : TIKA_TO_STANDARD_MIME.getOrDefault(tikaOutput, tikaOutput);

        // Final check: extension must match MIME, and either client MIME or extension must confirm it
        String expectedMime = EXTENSION_TO_MIME.get(matchedExtension.get());
        return expectedMime.equals(normalizedTikaMime) &&
                (clientMime.equals(expectedMime) || originalName.endsWith(matchedExtension.get()));
    }

    private Optional<String> detectZipContainerMime(byte[] fileBytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase(Locale.ROOT);
                if (entryName.startsWith("word/")) {
                    return Optional.of(DOCX_MIME);
                }
                if (entryName.startsWith("xl/")) {
                    return Optional.of(XLSX_MIME);
                }
                if (entryName.startsWith("ppt/")) {
                    return Optional.of(PPTX_MIME);
                }
                if ("mimetype".equals(entryName)) {
                    String mimetype = new String(zip.readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (ODT_MIME.equals(mimetype)) {
                        return Optional.of(ODT_MIME);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
