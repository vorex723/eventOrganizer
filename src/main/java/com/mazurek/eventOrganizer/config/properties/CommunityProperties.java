package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import org.springframework.util.unit.DataSize;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.community")
public class CommunityProperties {

    @Min(1)
    @Max(100)
    private int maxTagsPerEvent = 10;

    @Min(1)
    @Max(10_000)
    private int maxFilesPerEvent = 50;

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

    @NotNull
    private DataSize maxEventFileStorage = DataSize.ofMegabytes(500);

    @Min(1)
    @Max(255)
    private int maxDisplayFilenameLength = 120;

    @Min(1)
    @Max(100_000)
    private int maxAttendees = 1_000;
}
