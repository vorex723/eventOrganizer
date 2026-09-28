package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.config.properties.CommunityProperties;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
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

    private static final int ZIP_READ_BUFFER_SIZE = 8 * 1024;
    private static final int MAX_MIMETYPE_BYTES = 512;

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
    private final CommunityProperties communityProperties;

    public boolean isFileCorrect(MultipartFile uploadedFile) throws IOException {
        return detectValidatedContentType(uploadedFile).isPresent();
    }

    public Optional<String> detectValidatedContentType(MultipartFile uploadedFile) throws IOException {
        if (uploadedFile == null )
            return Optional.empty();

        if (uploadedFile.isEmpty())
            throw new EmptyUploadedFileException();


        return detectValidatedContentType(uploadedFile.getOriginalFilename(), uploadedFile.getBytes());
    }

    public Optional<String> detectValidatedContentType(String originalFilename, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            return Optional.empty();
        }

        String originalName = Optional.ofNullable(originalFilename)
                .orElse("")
                .toLowerCase(Locale.ROOT)
                .trim();

        // Extract extension from filename
        Optional<String> matchedExtension = EXTENSION_TO_MIME.keySet().stream()
                .filter(originalName::endsWith)
                .findFirst();

        if (matchedExtension.isEmpty())
            return Optional.empty();

        String tikaOutput = tikaFileTypeDetector.detect(fileBytes, originalName).toLowerCase(Locale.ROOT);
        String normalizedTikaMime = ZIP_CONTAINER_MIME_TYPES.contains(tikaOutput)
                ? detectZipContainerMime(fileBytes).orElse(tikaOutput)
                : TIKA_TO_STANDARD_MIME.getOrDefault(tikaOutput, tikaOutput);

        String expectedMime = EXTENSION_TO_MIME.get(matchedExtension.get());
        if (!expectedMime.equals(normalizedTikaMime)) {
            return Optional.empty();
        }

        return Optional.of(expectedMime);
    }

    private Optional<String> detectZipContainerMime(byte[] fileBytes) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry;
            int entryCount = 0;
            long totalUncompressedBytes = 0;
            String detectedMime = null;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entryCount > communityProperties.getMaxArchiveEntries()) {
                    return Optional.empty();
                }
                String entryName = entry.getName().toLowerCase(Locale.ROOT);
                if (entryName.startsWith("word/")) {
                    detectedMime = DOCX_MIME;
                }
                if (entryName.startsWith("xl/")) {
                    detectedMime = XLSX_MIME;
                }
                if (entryName.startsWith("ppt/")) {
                    detectedMime = PPTX_MIME;
                }
                ByteArrayOutputStream mimetypeBytes = "mimetype".equals(entryName)
                        ? new ByteArrayOutputStream(MAX_MIMETYPE_BYTES)
                        : null;
                byte[] buffer = new byte[ZIP_READ_BUFFER_SIZE];
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    totalUncompressedBytes += read;
                    if (exceedsArchiveLimits(totalUncompressedBytes, fileBytes.length)) {
                        return Optional.empty();
                    }
                    if (mimetypeBytes != null && mimetypeBytes.size() < MAX_MIMETYPE_BYTES) {
                        mimetypeBytes.write(buffer, 0, Math.min(read, MAX_MIMETYPE_BYTES - mimetypeBytes.size()));
                    }
                }
                if (mimetypeBytes != null
                        && ODT_MIME.equals(mimetypeBytes.toString(StandardCharsets.UTF_8).trim())) {
                    detectedMime = ODT_MIME;
                }
            }
            return Optional.ofNullable(detectedMime);
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private boolean exceedsArchiveLimits(long totalUncompressedBytes, long compressedBytes) {
        return totalUncompressedBytes > communityProperties.getMaxArchiveUncompressedSize().toBytes()
                || (double) totalUncompressedBytes
                > (double) compressedBytes * communityProperties.getMaxArchiveCompressionRatio();
    }
}
