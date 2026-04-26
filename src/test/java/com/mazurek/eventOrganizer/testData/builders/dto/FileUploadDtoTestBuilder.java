package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.file.FileUploadDto;
import org.springframework.web.multipart.MultipartFile;

import static com.mazurek.eventOrganizer.testData.TestConstants.FileConstants.MALWARE_FILE_USER_FILE_NAME;
import static com.mazurek.eventOrganizer.testData.TestConstants.FileConstants.USER_FILE_NAME;

public class FileUploadDtoTestBuilder {

    private String userFilename = USER_FILE_NAME;
    private MultipartFile file = MultipartFileTestBuilder.jpgFile().buildMultipartFile();

    public static FileUploadDtoTestBuilder jpgFile() {
        return new FileUploadDtoTestBuilder()
                .userFilename(USER_FILE_NAME)
                .file(MultipartFileTestBuilder.jpgFile().buildMultipartFile());
    }

    public static FileUploadDtoTestBuilder emptyJpgFile() {
        return new FileUploadDtoTestBuilder()
                .userFilename(USER_FILE_NAME)
                .file(MultipartFileTestBuilder.jpgFile()
                        .content(new byte[0])
                        .buildMultipartFile());
    }

    public static FileUploadDtoTestBuilder malwareFile() {
        return new FileUploadDtoTestBuilder()
                .userFilename(MALWARE_FILE_USER_FILE_NAME)
                .file(MultipartFileTestBuilder.malwareFile().buildMultipartFile());
    }

    public FileUploadDtoTestBuilder userFilename(String userFilename) {
        this.userFilename = userFilename;
        return this;
    }

    public FileUploadDtoTestBuilder file(MultipartFile file) {
        this.file = file;
        return this;
    }

    public FileUploadDto build() {
        return FileUploadDto.builder()
                .userFilename(userFilename)
                .file(file)
                .build();
    }
}
