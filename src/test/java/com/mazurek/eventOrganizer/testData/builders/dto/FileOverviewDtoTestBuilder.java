package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.file.FileOverviewDto;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.FileConstants.JPG_FILE_ID;

/** Fresh service-return fixture; defaults reuse the corresponding domain builder. */
public class FileOverviewDtoTestBuilder {
    private UUID id = JPG_FILE_ID;

    public FileOverviewDtoTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public FileOverviewDto build() {
        return new FileOverviewDto(FileTestBuilder.jpgFile().id(id).build());
    }
}
