package com.mazurek.eventOrganizer.file;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileUploadDto {
    private String userFileName;
    private MultipartFile file;
}
