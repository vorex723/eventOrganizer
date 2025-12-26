package com.mazurek.eventOrganizer.file;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileOverviewDto {
    private UUID id;
    private String userFileName;
    private String originalFilename;
    private String fileContentType;
    private ZonedDateTime uploadDateTime;
    private UserProfileDto owner;

    public FileOverviewDto(File file) {
        this.id = file.getId();
        this.userFileName = file.getUserFileName();
        this.originalFilename = file.getOriginalFileName();
        this.fileContentType = file.getContentType();
        this.owner = new UserProfileDto(file.getOwner());
        this.uploadDateTime = file.getUploadDateTime();
    }
}
