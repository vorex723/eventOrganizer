package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.testData.TestFileContentFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.testData.TestConstants.FileConstants.*;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileTestBuilder unit tests:")
class FileTestBuilderUnitTest {

    @ParameterizedTest(name = "Buffer isolation for {0}")
    @MethodSource("filePresets")
    void whenReusingPresetBuilderShouldCopyContentForEachBuild(FilePreset preset) {
        var builder = preset.newBuilder().get();
        var first = builder.build();
        assertThat(first.getContent()).isNotEmpty();
        byte[] expected = first.getContent().clone();
        var second = builder.build();
        first.getContent()[0] ^= 1;

        assertThat(second.getContent()).isNotSameAs(first.getContent()).containsExactly(expected);
        assertThat(builder.build().getContent()).containsExactly(expected);
    }

    @Test
    void whenProvidingContentShouldSnapshotInputWithoutSharingMutableBuffer() {
        byte[] input = TestFileContentFactory.jpg();
        assertThat(input).isNotEmpty();
        var builder = FileTestBuilder.jpgFile().content(input);
        input[0] ^= 1;
        var file = builder.build();

        assertThat(file.getContent()).isNotSameAs(input).containsExactly(TestFileContentFactory.jpg());
        file.getContent()[0] ^= 1;
        assertThat(builder.build().getContent()).containsExactly(TestFileContentFactory.jpg());
    }

    @Test
    void whenProvidingParentAndOwnerShouldKeepReferencesWithoutChangingParentCollection() {
        var event = EventTestBuilder.firstEvent().build();
        var owner = UserTestBuilder.firstUser().build();
        var existing = FileTestBuilder.pdfFile().event(event).owner(owner).build();
        event.addFile(existing);
        var file = FileTestBuilder.jpgFile().event(event).owner(owner).build();

        assertThat(file.getEvent()).isSameAs(event);
        assertThat(file.getOwner()).isSameAs(owner);
        assertThat(event.getFiles()).containsExactly(existing);
    }

    @Test
    void whenProvidingNullRelationsAndContentShouldBuildWithoutDereferencingParent() {
        var first = FileTestBuilder.jpgFile().event(null).content(null).owner(null).build();
        var reversed = FileTestBuilder.jpgFile().owner(null).content(null).event(null).build();
        for (var file : java.util.List.of(first, reversed)) {
            assertThat(file.getEvent()).isNull();
            assertThat(file.getOwner()).isNull();
            assertThat(file.getContent()).isNull();
        }
    }

    @ParameterizedTest(name = "Fresh fixtures for {0}")
    @MethodSource("filePresets")
    void whenUsingFreshPresetBuildersShouldKeepFilesGraphsAndContentIndependent(FilePreset preset) {
        File first = preset.newBuilder().get().build();
        File second = preset.newBuilder().get().build();
        assertThat(first.getContent()).isNotEmpty();
        assertThat(second.getContent()).isNotEmpty();
        byte[] secondContent = second.getContent().clone();
        String secondDisplayName = second.getUserFileName();

        assertThat(first).isNotSameAs(second);
        assertThat(first.getEvent()).isNotSameAs(second.getEvent());
        assertThat(first.getOwner()).isNotSameAs(second.getOwner());
        assertThat(first.getContent()).isNotSameAs(second.getContent()).isNotEmpty();
        assertThat(second.getId()).isEqualTo(preset.id());
        assertThat(second.getOriginalFileName()).isEqualTo(preset.originalName());
        assertThat(second.getContentType()).isEqualTo(preset.contentType());
        assertThat(second.getUploadDateTime()).isEqualTo(TimeConstants.NOW);

        first.getContent()[0] ^= 1;
        first.setUserFileName("Changed only in the first fixture");
        first.getOwner().setFirstName("Changed owner");

        assertThat(second.getContent()).containsExactly(secondContent);
        assertThat(second.getUserFileName()).isEqualTo(secondDisplayName);
        assertThat(second.getOwner().getFirstName()).isNotEqualTo("Changed owner");
    }

    @ParameterizedTest(name = "Generated ID override for {0}")
    @MethodSource("filePresets")
    void whenPreparingNewPersistentFileShouldHonorExplicitNullId(FilePreset preset) {
        File file = preset.newBuilder().get().id(null).build();

        assertThat(file.getId()).isNull();
        assertThat(file.getOriginalFileName()).isEqualTo(preset.originalName());
        assertThat(file.getContentType()).isEqualTo(preset.contentType());
    }

    @Test
    void whenUsingDefaultBuilderShouldKeepOriginalJpgDataAndLiteralDisplayName() {
        File file = new FileTestBuilder().build();

        assertThat(file.getId()).isEqualTo(JPG_FILE_ID);
        assertThat(file.getUserFileName()).isEqualTo(FIRST_FILE_USER_NAME);
        assertThat(file.getOriginalFileName()).isEqualTo(JPG_FILE_ORIGINAL_NAME);
        assertThat(file.getContentType()).isEqualTo(JPG_FILE_CONTENT_TYPE);
        assertThat(file.getContent()).containsExactly(TestFileContentFactory.jpg());
        assertThat(file.getOwner().getId()).isEqualTo(UserConstants.FIRST_USER_ID);
    }

    @Test
    void whenBuildingJpgAndJpegShouldKeepDistinctIdentitiesNamesAndOwners() {
        File jpg = FileTestBuilder.jpgFile().build();
        File jpeg = FileTestBuilder.jpegFile().build();

        assertThat(jpg.getId()).isEqualTo(JPG_FILE_ID).isNotEqualTo(jpeg.getId());
        assertThat(jpeg.getId()).isEqualTo(JPEG_FILE_ID);
        assertThat(jpg.getOriginalFileName()).isEqualTo(JPG_FILE_ORIGINAL_NAME);
        assertThat(jpeg.getOriginalFileName()).isEqualTo(JPEG_FILE_ORIGINAL_NAME);
        assertThat(jpg.getUserFileName()).isNotEqualTo(jpeg.getUserFileName());
        assertThat(jpg.getOwner().getId()).isEqualTo(UserConstants.FIRST_USER_ID);
        assertThat(jpeg.getOwner().getId()).isEqualTo(UserConstants.SECOND_USER_ID);
        // JPEG and JPG intentionally share MIME/signature, not fixture identity.
        assertThat(jpg.getContentType()).isEqualTo(JPG_FILE_CONTENT_TYPE);
        assertThat(jpeg.getContentType()).isEqualTo(JPEG_FILE_CONTENT_TYPE);
    }

    static Stream<FilePreset> filePresets() {
        // Case metadata holds factories, never mutable entities or shared byte arrays.
        return Stream.of(
                new FilePreset("jpg", FileTestBuilder::jpgFile, JPG_FILE_ID, JPG_FILE_ORIGINAL_NAME, JPG_FILE_CONTENT_TYPE),
                new FilePreset("jpeg", FileTestBuilder::jpegFile, JPEG_FILE_ID, JPEG_FILE_ORIGINAL_NAME, JPEG_FILE_CONTENT_TYPE),
                new FilePreset("png", FileTestBuilder::pngFile, PNG_FILE_ID, PNG_FILE_ORIGINAL_NAME, PNG_FILE_CONTENT_TYPE),
                new FilePreset("pdf", FileTestBuilder::pdfFile, PDF_FILE_ID, PDF_FILE_ORIGINAL_NAME, PDF_FILE_CONTENT_TYPE),
                new FilePreset("doc", FileTestBuilder::docFile, DOC_FILE_ID, DOC_FILE_ORIGINAL_NAME, DOC_FILE_CONTENT_TYPE),
                new FilePreset("docx", FileTestBuilder::docxFile, DOCX_FILE_ID, DOCX_FILE_ORIGINAL_NAME, DOCX_FILE_CONTENT_TYPE),
                new FilePreset("odt", FileTestBuilder::odtFile, ODT_FILE_ID, ODT_FILE_ORIGINAL_NAME, ODT_FILE_CONTENT_TYPE),
                new FilePreset("ppt", FileTestBuilder::pptFile, PPT_FILE_ID, PPT_FILE_ORIGINAL_NAME, PPT_FILE_CONTENT_TYPE),
                new FilePreset("pptx", FileTestBuilder::pptxFile, PPTX_FILE_ID, PPTX_FILE_ORIGINAL_NAME, PPTX_FILE_CONTENT_TYPE),
                new FilePreset("xls", FileTestBuilder::xlsFile, XLS_FILE_ID, XLS_FILE_ORIGINAL_NAME, XLS_FILE_CONTENT_TYPE),
                new FilePreset("xlsx", FileTestBuilder::xlsxFile, XLSX_FILE_ID, XLSX_FILE_ORIGINAL_NAME, XLSX_FILE_CONTENT_TYPE),
                new FilePreset("mp4", FileTestBuilder::mp4File, MP4_FILE_ID, MP4_FILE_ORIGINAL_NAME, MP4_FILE_CONTENT_TYPE),
                new FilePreset("avi", FileTestBuilder::aviFile, AVI_FILE_ID, AVI_FILE_ORIGINAL_NAME, AVI_FILE_CONTENT_TYPE)
        );
    }

    private record FilePreset(String extension, Supplier<FileTestBuilder> newBuilder, UUID id,
                              String originalName, String contentType) {
        @Override
        public String toString() {
            return extension;
        }
    }
}
