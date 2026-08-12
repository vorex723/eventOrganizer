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

    private UUID id = FileConstants.FIRST_FILE_ID;
    private String userFileName = FileConstants.FIRST_FILE_USER_NAME;
    private String originalFileName = FileConstants.FIRST_FILE_ORIGINAL_NAME;
    private byte[] content = TestFileContentFactory.jpeg();
    private String contentType = FileConstants.FIRST_FILE_CONTENT_TYPE;
    private Instant uploadDateTime = TimeConstants.NOW;
    private Event event = EventTestBuilder.firstEvent().build();
    private User owner = UserTestBuilder.firstUser().build();
    private String ownerNameAtCreation = UserConstants.FIRST_USER_FULL_NAME;

    // ==================== IMAGE FILES ====================

    /**
     * Creates a JPG image file (image/jpeg)
     */
    public static FileTestBuilder jpgFile() {
        return new FileTestBuilder()
                .id(FileConstants.JPG_FILE_ID)
                .userFileName("test-photo-" + FileConstants.JPG_FILE_ID + ".jpg")
                .originalFileName(FileConstants.JPG_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.jpg())
                .contentType(FileConstants.JPG_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build())
                .ownerNameAtCreation(UserConstants.FIRST_USER_FULL_NAME);
    }

    /**
     * Creates a JPEG image file (image/jpeg) - alias for jpgFile()
     */
    public static FileTestBuilder jpegFile() {
        return new FileTestBuilder()
                .id(FileConstants.JPEG_FILE_ID)
                .userFileName("test-photo-" + FileConstants.JPEG_FILE_ID + ".jpeg")
                .originalFileName(FileConstants.JPEG_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.jpeg())
                .contentType(FileConstants.JPEG_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build())
                .ownerNameAtCreation(UserConstants.SECOND_USER_FULL_NAME);
    }

    /**
     * Creates a PNG image file (image/png)
     */
    public static FileTestBuilder pngFile() {
        return new FileTestBuilder()
                .id(FileConstants.PNG_FILE_ID)
                .userFileName("test-image-" + FileConstants.PNG_FILE_ID + ".png")
                .originalFileName(FileConstants.PNG_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.png())
                .contentType(FileConstants.PNG_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build())
                .ownerNameAtCreation(UserConstants.FIRST_USER_FULL_NAME);
    }

    // ==================== DOCUMENT FILES ====================

    /**
     * Creates a PDF file (application/pdf)
     */
    public static FileTestBuilder pdfFile() {
        return new FileTestBuilder()
                .id(FileConstants.PDF_FILE_ID)
                .userFileName("test-document-" + FileConstants.PDF_FILE_ID + ".pdf")
                .originalFileName(FileConstants.PDF_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.pdf())
                .contentType(FileConstants.PDF_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build())
                .ownerNameAtCreation(UserConstants.SECOND_USER_FULL_NAME);
    }

    /**
     * Creates an old DOC file (application/msword)
     */
    public static FileTestBuilder docFile() {
        return new FileTestBuilder()
                .id(FileConstants.DOC_FILE_ID)
                .userFileName("test-document-" + FileConstants.DOC_FILE_ID + ".doc")
                .originalFileName(FileConstants.DOC_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.doc())
                .contentType(FileConstants.DOC_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.thirdUser().build())
                .ownerNameAtCreation(UserConstants.THIRD_USER_FULL_NAME);
    }

    /**
     * Creates a DOCX file (application/vnd.openxmlformats-officedocument.wordprocessingml.document)
     */
    public static FileTestBuilder docxFile() {
        return new FileTestBuilder()
                .id(FileConstants.DOCX_FILE_ID)
                .userFileName("test-document-" + FileConstants.DOCX_FILE_ID + ".docx")
                .originalFileName(FileConstants.DOCX_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.docx())
                .contentType(FileConstants.DOCX_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build())
                .ownerNameAtCreation(UserConstants.FIRST_USER_FULL_NAME);
    }

    /**
     * Creates an ODT file (application/vnd.oasis.opendocument.text)
     */
    public static FileTestBuilder odtFile() {
        return new FileTestBuilder()
                .id(FileConstants.ODT_FILE_ID)
                .userFileName("test-document-" + FileConstants.ODT_FILE_ID + ".odt")
                .originalFileName(FileConstants.ODT_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.odt())
                .contentType(FileConstants.ODT_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build())
                .ownerNameAtCreation(UserConstants.SECOND_USER_FULL_NAME);
    }

    // ==================== PRESENTATION FILES ====================

    /**
     * Creates an old PPT file (application/vnd.ms-powerpoint)
     */
    public static FileTestBuilder pptFile() {
        return new FileTestBuilder()
                .id(FileConstants.PPT_FILE_ID)
                .userFileName("test-presentation-" + FileConstants.PPT_FILE_ID + ".ppt")
                .originalFileName(FileConstants.PPT_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.ppt())
                .contentType(FileConstants.PPT_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.thirdUser().build())
                .ownerNameAtCreation(UserConstants.THIRD_USER_FULL_NAME);
    }

    /**
     * Creates a PPTX file (application/vnd.openxmlformats-officedocument.presentationml.presentation)
     */
    public static FileTestBuilder pptxFile() {
        return new FileTestBuilder()
                .id(FileConstants.PPTX_FILE_ID)
                .userFileName("test-presentation-" + FileConstants.PPTX_FILE_ID + ".pptx")
                .originalFileName(FileConstants.PPTX_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.pptx())
                .contentType(FileConstants.PPTX_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build())
                .ownerNameAtCreation(UserConstants.FIRST_USER_FULL_NAME);
    }

    // ==================== SPREADSHEET FILES ====================

    /**
     * Creates an old XLS file (application/vnd.ms-excel)
     */
    public static FileTestBuilder xlsFile() {
        return new FileTestBuilder()
                .id(FileConstants.XLS_FILE_ID)
                .userFileName("test-spreadsheet-" + FileConstants.XLS_FILE_ID + ".xls")
                .originalFileName(FileConstants.XLS_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.xls())
                .contentType(FileConstants.XLS_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build())
                .ownerNameAtCreation(UserConstants.SECOND_USER_FULL_NAME);
    }

    /**
     * Creates an XLSX file (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet)
     */
    public static FileTestBuilder xlsxFile() {
        return new FileTestBuilder()
                .id(FileConstants.XLSX_FILE_ID)
                .userFileName("test-spreadsheet-" + FileConstants.XLSX_FILE_ID + ".xlsx")
                .originalFileName(FileConstants.XLSX_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.xlsx())
                .contentType(FileConstants.XLSX_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.thirdUser().build())
                .ownerNameAtCreation(UserConstants.THIRD_USER_FULL_NAME);
    }

    // ==================== VIDEO FILES ====================

    /**
     * Creates an MP4 video file (video/mp4)
     */
    public static FileTestBuilder mp4File() {
        return new FileTestBuilder()
                .id(FileConstants.MP4_FILE_ID)
                .userFileName("test-video-" + FileConstants.MP4_FILE_ID + ".mp4")
                .originalFileName(FileConstants.MP4_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.mp4())
                .contentType(FileConstants.MP4_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.firstUser().build())
                .ownerNameAtCreation(UserConstants.FIRST_USER_FULL_NAME);
    }

    /**
     * Creates an AVI video file (video/x-msvideo)
     */
    public static FileTestBuilder aviFile() {
        return new FileTestBuilder()
                .id(FileConstants.AVI_FILE_ID)
                .userFileName("test-video-" + FileConstants.AVI_FILE_ID + ".avi")
                .originalFileName(FileConstants.AVI_FILE_ORIGINAL_NAME)
                .content(TestFileContentFactory.avi())
                .contentType(FileConstants.AVI_FILE_CONTENT_TYPE)
                .owner(UserTestBuilder.secondUser().build())
                .ownerNameAtCreation(UserConstants.SECOND_USER_FULL_NAME);
    }

    // ==================== BACKWARD COMPATIBILITY ALIASES ====================

    /**
     * Default first file (JPG image)
     */
    public static FileTestBuilder firstFile() {
        return jpgFile()
                .id(FileConstants.FIRST_FILE_ID)
                .userFileName(FileConstants.FIRST_FILE_USER_NAME)
                .originalFileName(FileConstants.FIRST_FILE_ORIGINAL_NAME);
    }

    /**
     * Default second file (PNG image)
     */
    public static FileTestBuilder secondFile() {
        return pngFile()
                .id(FileConstants.SECOND_FILE_ID)
                .userFileName(FileConstants.SECOND_FILE_USER_NAME)
                .originalFileName(FileConstants.SECOND_FILE_ORIGINAL_NAME);
    }

    /**
     * Default third file (PDF document)
     */
    public static FileTestBuilder thirdFile() {
        return pdfFile()
                .id(FileConstants.THIRD_FILE_ID)
                .userFileName(FileConstants.THIRD_FILE_USER_NAME)
                .originalFileName(FileConstants.THIRD_FILE_ORIGINAL_NAME);
    }

    /**
     * Alias for pdfFile() - for clarity in tests
     */
    public static FileTestBuilder documentFile() {
        return pdfFile();
    }

    /**
     * Alias for pngFile() - for clarity in tests
     */
    public static FileTestBuilder imageFile() {
        return pngFile();
    }

    /**
     * Alias for pptxFile() - for clarity in tests
     */
    public static FileTestBuilder presentationFile() {
        return pptxFile();
    }

    /**
     * Alias for xlsxFile() - for clarity in tests
     */
    public static FileTestBuilder spreadsheetFile() {
        return xlsxFile();
    }

    /**
     * Alias for mp4File() - for clarity in tests
     */
    public static FileTestBuilder videoFile() {
        return mp4File();
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
        this.content = content;
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
        this.ownerNameAtCreation = owner != null ? owner.getFullName() : null;
        return this;
    }

    public FileTestBuilder ownerNameAtCreation(String ownerNameAtCreation) {
        this.ownerNameAtCreation = ownerNameAtCreation;
        return this;
    }

    public File build() {
        File fileToReturn = File.builder()
                .id(id)
                .userFileName(userFileName)
                .originalFileName(originalFileName)
                .content(content)
                .contentType(contentType)
                .uploadDateTime(uploadDateTime)
                .event(event)
                .owner(owner)
                .ownerNameAtCreation(ownerNameAtCreation)
                .build();

        event.addFile(fileToReturn);
        owner.addFile(fileToReturn);

        return fileToReturn;
    }
}