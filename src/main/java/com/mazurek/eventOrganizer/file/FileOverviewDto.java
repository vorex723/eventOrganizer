package com.mazurek.eventOrganizer.file;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileOverviewDto {
    private UUID id;
    private String userFilename;
    private String originalFilename;
    private String fileContentType;
    private Instant uploadDateTime;
    private UserProfileDto owner;

    public FileOverviewDto(File file) {
        this.id = file.getId();
        this.userFilename = file.getUserFileName();
        this.originalFilename = file.getOriginalFileName();
        this.fileContentType = file.getContentType();
        this.owner = file.getOwner() == null ? UserProfileDto.deletedUser() : new UserProfileDto(file.getOwner());
        this.uploadDateTime = file.getUploadDateTime();
    }

    public FileOverviewDto(FileOverviewProjection file) {
        this.id = file.getId();
        this.userFilename = file.getUserFileName();
        this.originalFilename = file.getOriginalFileName();
        this.fileContentType = file.getContentType();
        this.uploadDateTime = file.getUploadDateTime();
        this.owner = file.getOwnerId() == null ? UserProfileDto.deletedUser() : new UserProfileDto(
                file.getOwnerId(),
                file.getOwnerFirstName(),
                file.getOwnerLastName(),
                file.getOwnerHomeCity()
        );
    }
}
