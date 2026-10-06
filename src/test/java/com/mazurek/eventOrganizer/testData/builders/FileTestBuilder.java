package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.testData.TestFileContentFactory;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.user.User;

import java.time.Instant;
import java.util.UUID;

import static  com.mazurek.eventOrganizer.testData.TestConstants.*;

/**
 * Builder for creating File test objects with sensible defaults.
 * Uses TestFileContentFactory to generate valid file content with correct magic numbers.
 *
 * ONLY includes file types allowed by FileUtils.EXTENSION_TO_MIME whitelist:
 * - Images: .jpg, .jpeg, .png
 * - Documents: .pdf, .doc, .docx, .odt
 * - Presentations: .ppt, .pptx
 * - Spreadsheets: .xls, .xlsx
 * - Videos: .mp4, .avi
 */
public class FileTestBuilder {

    private UUID id = FileConstants.JPG_FILE_ID;
    private String userFileName = FileConstants.FIRST_FILE_USER_NAME;
    private String originalFileName = FileConstants.JPG_FILE_ORIGINAL_NAME;
    private byte[] content = TestFileContentFactory.jpg();
    private String contentType = FileConstants.JPG_FILE_CONTENT_TYPE;
    private Instant uploadDateTime = TimeConstants.NOW;
    private Event event = EventTestBuilder.firstEvent().build();
    private User owner = UserTestBuilder.firstUser().build();

    // ==================== IMAGE FILES ====================

    /**
     * Creates a JPG image file (image/jpeg)
     */
    public static FileTestBuilder jpgFile() {
        return new FileTestBuilder()
                .id(FileConstants.JPG_FILE_ID)
                .userFileName(FileConstants.JPG_FILE_USER_NAME)
                .originalFileName(FileConstants.JPG_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.jpg())
                .contentType(FileConstants.JPG_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build());
    }

    /**
     * Creates a distinct JPEG fixture with its own ID, filename and second-user owner.
     */
    public static FileTestBuilder jpegFile() {
        return new FileTestBuilder()
                .id(FileConstants.JPEG_FILE_ID)
                .userFileName(FileConstants.JPEG_FILE_USER_NAME)
                .originalFileName(FileConstants.JPEG_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.jpeg())
                .contentType(FileConstants.JPEG_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build());
    }

    /**
     * Creates a PNG image file (image/png)
     */
    public static FileTestBuilder pngFile() {
        return new FileTestBuilder()
                .id(FileConstants.PNG_FILE_ID)
                .userFileName(FileConstants.PNG_FILE_USER_NAME)
                .originalFileName(FileConstants.PNG_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.png())
                .contentType(FileConstants.PNG_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build());
    }

    // ==================== DOCUMENT FILES ====================

    /**
     * Creates a PDF file (application/pdf)
     */
    public static FileTestBuilder pdfFile() {
        return new FileTestBuilder()
                .id(FileConstants.PDF_FILE_ID)
                .userFileName(FileConstants.PDF_FILE_USER_NAME)
                .originalFileName(FileConstants.PDF_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.pdf())
                .contentType(FileConstants.PDF_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build());
    }

    /**
     * Creates an old DOC file (application/msword)
     */
    public static FileTestBuilder docFile() {
        return new FileTestBuilder()
                .id(FileConstants.DOC_FILE_ID)
                .userFileName(FileConstants.DOC_FILE_USER_NAME)
                .originalFileName(FileConstants.DOC_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.doc())
                .contentType(FileConstants.DOC_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.thirdUser().build());
    }

    /**
     * Creates a DOCX file (application/vnd.openxmlformats-officedocument.wordprocessingml.document)
     */
    public static FileTestBuilder docxFile() {
        return new FileTestBuilder()
                .id(FileConstants.DOCX_FILE_ID)
                .userFileName(FileConstants.DOCX_FILE_USER_NAME)
                .originalFileName(FileConstants.DOCX_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.docx())
                .contentType(FileConstants.DOCX_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build());
    }

    /**
     * Creates an ODT file (application/vnd.oasis.opendocument.text)
     */
    public static FileTestBuilder odtFile() {
        return new FileTestBuilder()
                .id(FileConstants.ODT_FILE_ID)
                .userFileName(FileConstants.ODT_FILE_USER_NAME)
                .originalFileName(FileConstants.ODT_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.odt())
                .contentType(FileConstants.ODT_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build());
    }

    // ==================== PRESENTATION FILES ====================

    /**
     * Creates an old PPT file (application/vnd.ms-powerpoint)
     */
    public static FileTestBuilder pptFile() {
        return new FileTestBuilder()
                .id(FileConstants.PPT_FILE_ID)
                .userFileName(FileConstants.PPT_FILE_USER_NAME)
                .originalFileName(FileConstants.PPT_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.ppt())
                .contentType(FileConstants.PPT_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.thirdUser().build());
    }

    /**
     * Creates a PPTX file (application/vnd.openxmlformats-officedocument.presentationml.presentation)
     */
    public static FileTestBuilder pptxFile() {
        return new FileTestBuilder()
                .id(FileConstants.PPTX_FILE_ID)
                .userFileName(FileConstants.PPTX_FILE_USER_NAME)
                .originalFileName(FileConstants.PPTX_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.pptx())
                .contentType(FileConstants.PPTX_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build());
    }

    // ==================== SPREADSHEET FILES ====================

    /**
     * Creates an old XLS file (application/vnd.ms-excel)
     */
    public static FileTestBuilder xlsFile() {
        return new FileTestBuilder()
                .id(FileConstants.XLS_FILE_ID)
                .userFileName(FileConstants.XLS_FILE_USER_NAME)
                .originalFileName(FileConstants.XLS_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.xls())
                .contentType(FileConstants.XLS_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build());
    }

    /**
     * Creates an XLSX file (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet)
     */
    public static FileTestBuilder xlsxFile() {
        return new FileTestBuilder()
                .id(FileConstants.XLSX_FILE_ID)
                .userFileName(FileConstants.XLSX_FILE_USER_NAME)
                .originalFileName(FileConstants.XLSX_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.xlsx())
                .contentType(FileConstants.XLSX_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.thirdUser().build());
    }

    // ==================== VIDEO FILES ====================

    /**
     * Creates an MP4 video file (video/mp4)
     */
    public static FileTestBuilder mp4File() {
        return new FileTestBuilder()
                .id(FileConstants.MP4_FILE_ID)
                .userFileName(FileConstants.MP4_FILE_USER_NAME)
                .originalFileName(FileConstants.MP4_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.mp4())
                .contentType(FileConstants.MP4_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build());
    }

    /**
     * Creates an AVI video file (video/x-msvideo)
     */
    public static FileTestBuilder aviFile() {
        return new FileTestBuilder()
                .id(FileConstants.AVI_FILE_ID)
                .userFileName(FileConstants.AVI_FILE_USER_NAME)
                .originalFileName(FileConstants.AVI_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.avi())
                .contentType(FileConstants.AVI_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build());
    }

    // ==================== BUILDER METHODS ====================

    public FileTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public FileTestBuilder userFileName(String userFileName) {
        this.userFileName = userFileName;
        return this;
    }

    public FileTestBuilder originalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
        return this;
    }

    public FileTestBuilder content(byte[] content) {
        this.content = content == null ? null : content.clone();
        return this;
    }

    public FileTestBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public FileTestBuilder uploadDateTime(Instant uploadDateTime) {
        this.uploadDateTime = uploadDateTime;
        return this;
    }

    public FileTestBuilder event(Event event) {
        this.event = event;
        return this;
    }

    public FileTestBuilder owner(User owner) {
        this.owner = owner;
        return this;
    }

    public File build() {
        return File.builder()
                .id(id)
                .userFileName(userFileName)
                .originalFileName(originalFileName)
                .content(content == null ? null : content.clone())
                .contentType(contentType)
                .uploadDateTime(uploadDateTime)
                .event(event)
                .owner(owner)
                .build();
    }
}
