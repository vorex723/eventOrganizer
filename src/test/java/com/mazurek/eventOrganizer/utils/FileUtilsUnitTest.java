package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.testData.TestFileContentFactory;
import com.mazurek.eventOrganizer.config.properties.CommunityProperties;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.testData.TestConstants;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import org.apache.tika.Tika;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileUtils unit tests:")
class FileUtilsUnitTest {

    @Mock
    private Tika tika;

    @Spy
    private CommunityProperties communityProperties = new CommunityProperties();

    @InjectMocks
    private FileUtils fileUtils;

    @Nested
    @DisplayName("File validation tests:")
    class IsFileCorrectTests {

        @Test
        @DisplayName("When uploaded file is null should return false")
        void whenUploadedFileIsNullShouldReturnFalse() throws IOException {
            assertThat(fileUtils.isFileCorrect(null)).isFalse();
        }

        @Test
        @DisplayName("When uploaded file is empty should throw EmptyUploadedFileException")
        void whenUploadedFileIsEmptyShouldThrowEmptyUploadedFileException() {
            MockMultipartFile emptyJpg = MultipartFileTestBuilder.jpgFile()
                    .content(new byte[]{})
                    .buildMultipartFile();

            assertThatThrownBy(() -> fileUtils.isFileCorrect(emptyJpg))
                    .isInstanceOf(EmptyUploadedFileException.class);
        }

        @Test
        @DisplayName("When file extension is not allowed should return false")
        void whenFileExtensionIsNotAllowedShouldReturnFalse() throws IOException {
            MockMultipartFile malwareFile = MultipartFileTestBuilder.malwareFile().buildMultipartFile();

            assertThat(fileUtils.isFileCorrect(malwareFile)).isFalse();
            verify(tika, never()).detect(any(byte[].class), anyString());
        }

        @Test
        @DisplayName("When extension and detected mime type match should return true")
        void whenExtensionAndDetectedMimeTypeMatchShouldReturnTrue() throws IOException {
            MockMultipartFile jpgFile = MultipartFileTestBuilder.jpgFile().buildMultipartFile();
            when(tika.detect(any(byte[].class), anyString()))
                    .thenReturn(TestConstants.FileConstants.JPG_FILE_CONTENT_TYPE);

            boolean result = fileUtils.isFileCorrect(jpgFile);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("When extension and detected mime type match should return validated normalized mime")
        void whenExtensionAndDetectedMimeTypeMatchShouldReturnValidatedNormalizedMime() throws IOException {
            MockMultipartFile jpgFile = MultipartFileTestBuilder.jpgFile()
                    .contentType(TestConstants.FileConstants.PDF_FILE_CONTENT_TYPE)
                    .buildMultipartFile();
            when(tika.detect(any(byte[].class), anyString()))
                    .thenReturn(TestConstants.FileConstants.JPG_FILE_CONTENT_TYPE);

            assertThat(fileUtils.detectValidatedContentType(jpgFile))
                    .contains(TestConstants.FileConstants.JPG_FILE_CONTENT_TYPE);
        }

        @Test
        @DisplayName("When extension and detected mime type mismatch should return false")
        void whenExtensionAndDetectedMimeTypeMismatchShouldReturnFalse() throws IOException {
            MockMultipartFile jpgFile = MultipartFileTestBuilder.jpgFile().buildMultipartFile();
            when(tika.detect(any(byte[].class), anyString()))
                    .thenReturn(TestConstants.FileConstants.PDF_FILE_CONTENT_TYPE);

            boolean result = fileUtils.isFileCorrect(jpgFile);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("When file name uses uppercase extension should still validate successfully")
        void whenFileNameUsesUppercaseExtensionShouldStillValidateSuccessfully() throws IOException {
            MockMultipartFile uppercaseJpgFile = MultipartFileTestBuilder.jpgFile()
                    .originalFileName(TestConstants.FileConstants.JPG_FILE_ORIGINAL_NAME.toUpperCase())
                    .buildMultipartFile();
            when(tika.detect(any(byte[].class), anyString()))
                    .thenReturn(TestConstants.FileConstants.JPG_FILE_CONTENT_TYPE);

            boolean result = fileUtils.isFileCorrect(uppercaseJpgFile);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("When tika returns generic zip for valid docx should map to allowed office mime and return true")
        void whenTikaReturnsGenericZipForValidDocxShouldMapToAllowedOfficeMimeAndReturnTrue() throws IOException {
            MockMultipartFile docxFile = MultipartFileTestBuilder.docxFile().buildMultipartFile();
            when(tika.detect(any(byte[].class), anyString())).thenReturn(TestConstants.FileConstants.MALWARE_FILE_CONTENT_TYPE);

            boolean result = fileUtils.isFileCorrect(docxFile);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("When tika returns generic zip for docx without office structure should return false")
        void whenTikaReturnsGenericZipForDocxWithoutOfficeStructureShouldReturnFalse() throws IOException {
            MockMultipartFile invalidDocxFile = MultipartFileTestBuilder.docxFile()
                    .content(TestFileContentFactory.malwareContent())
                    .buildMultipartFile();
            when(tika.detect(any(byte[].class), anyString())).thenReturn(TestConstants.FileConstants.MALWARE_FILE_CONTENT_TYPE);

            boolean result = fileUtils.isFileCorrect(invalidDocxFile);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("When a ZIP exceeds the entry limit should reject it")
        void whenZipExceedsEntryLimitShouldReturnFalse() throws IOException {
            communityProperties.setMaxArchiveEntries(2);
            MockMultipartFile archive = zipFile(zipWithEntries(3, 1));
            when(tika.detect(any(byte[].class), anyString())).thenReturn(TestConstants.FileConstants.MALWARE_FILE_CONTENT_TYPE);

            assertThat(fileUtils.isFileCorrect(archive)).isFalse();
        }

        @Test
        @DisplayName("When a ZIP exceeds its uncompressed inspection limit should reject it even after an office entry")
        void whenZipExceedsUncompressedLimitShouldReturnFalse() throws IOException {
            communityProperties.setMaxArchiveUncompressedSize(DataSize.ofBytes(64));
            communityProperties.setMaxArchiveCompressionRatio(10_000);
            MockMultipartFile archive = zipFile(zipWithEntries(1, 65));
            when(tika.detect(any(byte[].class), anyString())).thenReturn(TestConstants.FileConstants.MALWARE_FILE_CONTENT_TYPE);

            assertThat(fileUtils.isFileCorrect(archive)).isFalse();
        }

        @Test
        @DisplayName("When a ZIP exceeds the compression-ratio limit should reject it")
        void whenZipExceedsCompressionRatioShouldReturnFalse() throws IOException {
            communityProperties.setMaxArchiveUncompressedSize(DataSize.ofKilobytes(20));
            communityProperties.setMaxArchiveCompressionRatio(2);
            MockMultipartFile archive = zipFile(zipWithEntries(1, 10_000));
            when(tika.detect(any(byte[].class), anyString())).thenReturn(TestConstants.FileConstants.MALWARE_FILE_CONTENT_TYPE);

            assertThat(fileUtils.isFileCorrect(archive)).isFalse();
        }
    }

    private MockMultipartFile zipFile(byte[] content) {
        return new MockMultipartFile("file", "archive.docx", "application/zip", content);
    }

    private byte[] zipWithEntries(int entryCount, int entrySize) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            for (int index = 0; index < entryCount; index++) {
                String entryName = index == 0 ? "word/document.xml" : "word/part-" + index + ".xml";
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write(new byte[entrySize]);
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        }
    }
}
