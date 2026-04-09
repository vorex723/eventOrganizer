package com.mazurek.eventOrganizer.file;

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
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fileOverviewPageDto);
    }

    @GetMapping("/{eventId}/files/{fileId}")
    public ResponseEntity<FileOverviewDto> getFileOverviewFromEventByFileId(@PathVariable("eventId") UUID eventId,
                                                                            @PathVariable("fileId")UUID fileId)
    {
        FileOverviewDto fileOverviewToServe = fileService.getFileOverviewById(fileId,eventId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fileOverviewToServe);
    }

    @PostMapping("/{eventId}/files")
    public ResponseEntity<FileOverviewDto> uploadFileToEvent(@Valid @ModelAttribute FileUploadDto fileUploadDto,
                                                             @PathVariable("eventId") UUID eventId) throws IOException
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.uploadFileToEvent(fileUploadDto,eventId));
    }


    @GetMapping("/{eventId}/files/{fileId}/data")
    public ResponseEntity<byte[]> getFileDataFromEventByFileId(@PathVariable("eventId") UUID eventId,
                                                               @PathVariable("fileId")UUID fileId)
    {
        File fileToServe = fileService.getFileDataById(fileId, eventId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(fileToServe.getContentType())).body(fileToServe.getContent());
    }
}
