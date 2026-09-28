package com.mazurek.eventOrganizer.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class FileController {

    private final FileService fileService;

    @GetMapping("/{eventId}/files")
    public ResponseEntity<FileOverviewPageDto> getFileOverviewsPageFromEventByEventId(@PathVariable("eventId") UUID eventId,
                                                                                      @RequestParam(name = "page", required = false, defaultValue = "0") int pageNumber)
    {
        FileOverviewPageDto fileOverviewPageDto = fileService.getFileOverviewPageByEventId(eventId, pageNumber);
        return ResponseEntity.ok(fileOverviewPageDto);
    }

    @GetMapping("/{eventId}/files/{fileId}")
    public ResponseEntity<FileOverviewDto> getFileOverviewFromEventByFileId(@PathVariable("eventId") UUID eventId,
                                                                            @PathVariable("fileId")UUID fileId)
    {
        FileOverviewDto fileOverviewToServe = fileService.getFileOverviewById(fileId,eventId);
        return ResponseEntity.ok(fileOverviewToServe);
    }

    @Operation(summary = "Upload a file to an event")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
            @ApiResponse(
                    responseCode = "409",
                    description = "The event file quota has been reached",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiError")
                    )
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "The uploaded file exceeds the configured maximum size",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiError")
                    )
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "The uploaded file type is not allowed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiError")
                    )
            )
    })
    @PostMapping(value = "/{eventId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<FileOverviewDto> uploadFileToEvent(@Valid @ModelAttribute FileUploadDto fileUploadDto,
                                                             @PathVariable("eventId") UUID eventId) throws IOException
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.uploadFileToEvent(fileUploadDto,eventId));
    }


    @GetMapping("/{eventId}/files/{fileId}/data")
    @Operation(summary = "Download event file data", description = "Returns binary data using the uploaded file's original media type.")
    @ApiResponse(
            responseCode = "200",
            description = "File data",
            content = @Content(
                    mediaType = MediaType.ALL_VALUE,
                    schema = @Schema(type = "string", format = "binary")
            )
    )
    public ResponseEntity<byte[]> getFileDataFromEventByFileId(@PathVariable("eventId") UUID eventId,
                                                               @PathVariable("fileId")UUID fileId)
    {
        FileContentDto fileToServe = fileService.getFileDataById(fileId, eventId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(fileToServe.contentType())).body(fileToServe.content());
    }
}
