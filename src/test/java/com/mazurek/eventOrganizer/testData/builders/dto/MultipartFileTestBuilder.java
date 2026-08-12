package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.testData.TestFileContentFactory;
import org.springframework.mock.web.MockMultipartFile;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class MultipartFileTestBuilder {

    private String originalFileName = FileConstants.JPG_FILE_ORIGINAL_NAME;
    private String contentType = FileConstants.JPG_FILE_CONTENT_TYPE;
    private byte[] content = TestFileContentFactory.jpg();

    public static MultipartFileTestBuilder jpgFile() {
        return new MultipartFileTestBuilder()
                .originalFileName(FileConstants.JPG_FILE_ORIGINAL_NAME)
                .contentType(FileConstants.JPG_FILE_CONTENT_TYPE)
                .content(TestFileContentFactory.jpg());
    }
    public static MultipartFileTestBuilder malwareFile(){
        return new MultipartFileTestBuilder()
                .originalFileName(FileConstants.MALWARE_FILE_ORIGINAL_NAME)
                .contentType(FileConstants.MALWARE_FILE_CONTENT_TYPE)
                .content(TestFileContentFactory.malwareContent());
    }

    public static MultipartFileTestBuilder pngFile() {
        return new MultipartFileTestBuilder()
                .originalFileName(FileConstants.PNG_FILE_ORIGINAL_NAME)
                .contentType(FileConstants.PNG_FILE_CONTENT_TYPE)
                .content(TestFileContentFactory.png());
    }

    public static MultipartFileTestBuilder pdfFile() {
        return new MultipartFileTestBuilder()
                .originalFileName(FileConstants.PDF_FILE_ORIGINAL_NAME)
                .contentType(FileConstants.PDF_FILE_CONTENT_TYPE)
                .content(TestFileContentFactory.pdf());
    }

    public static MultipartFileTestBuilder docxFile() {
        return new MultipartFileTestBuilder()
                .originalFileName(FileConstants.DOCX_FILE_ORIGINAL_NAME)
                .contentType(FileConstants.DOCX_FILE_CONTENT_TYPE)
                .content(TestFileContentFactory.docx());
    }



    public MultipartFileTestBuilder originalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
        return this;
    }

    public MultipartFileTestBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public MultipartFileTestBuilder content(byte[] content) {
        this.content = content;
        return this;
    }

    public MockMultipartFile buildMultipartFile() {
        return new MockMultipartFile(FileConstants.FILE_MULTIPART_PART_NAME, originalFileName, contentType, content);
    }
}
