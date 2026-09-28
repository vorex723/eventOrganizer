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
    @Size(max = 120, message = "User filename cannot be longer than 120 characters.")
    @jakarta.validation.constraints.Pattern(
            regexp = "^[^\\p{Cntrl}/\\\\]+$",
            message = "User filename cannot contain path separators or control characters."
    )
    private String userFilename;
    @NotNull(message = "File must be provided.")
    private MultipartFile file;
}
