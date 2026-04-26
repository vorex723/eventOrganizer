package com.mazurek.eventOrganizer.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileUploadDto {
    @NotBlank(message = "User filename must be provided.")
    @Size(max = 255, message = "User filename cannot be longer than 255 characters.")
    private String userFilename;
    @NotNull(message = "File must be provided.")
    private MultipartFile file;
}
