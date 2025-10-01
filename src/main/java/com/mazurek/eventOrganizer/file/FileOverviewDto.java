package com.mazurek.eventOrganizer.file;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileOverviewDto {
    private UUID id;
    private String userFileName;
    private String originalFileName;
    private UserProfileDto owner;

    public FileOverviewDto(File file) {
        this.id = file.getId();
        this.userFileName = file.getUserFileName();
        this.originalFileName = file.getOriginalFileName();
        this.owner = new UserProfileDto(file.getOwner());
    }
}
