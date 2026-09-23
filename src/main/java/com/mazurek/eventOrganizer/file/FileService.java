package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.config.properties.CommunityProperties;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.file.EventFileQuotaExceededException;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final AuthenticationService authenticationService;
    private final NotificationCommandService notificationCommandService;
    private final EventRepository eventRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileUtils fileUtils;
    private final PaginationProperties paginationProperties;
    private final CommunityProperties communityProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FileOverviewDto getFileOverviewById(UUID fileId, UUID eventId) {
        User performingUser = authenticationService.getCurrentUser();
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (!event.isUserAttending(performingUser))
            throw new NotEventAttenderException();

        return new FileOverviewDto(fileRepository.findOverviewByIdAndEventId(fileId, eventId)
                .orElseThrow(FileNotFoundInEventException::new));
    }

    @Transactional(readOnly = true)
    public FileContentDto getFileDataById(UUID fileId, UUID eventId) {
        User performingUser = authenticationService.getCurrentUser();
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (!event.isUserAttending(performingUser))
            throw new NotEventAttenderException();

        FileContentProjection file = fileRepository.findContentByIdAndEventId(fileId, eventId)
                .orElseThrow(FileNotFoundInEventException::new);
        return new FileContentDto(file.getContentType(), file.getContent());
    }

    @Transactional(readOnly = true)
    public FileOverviewPageDto getFileOverviewPageByEventId(UUID eventId, int pageNumber) {
        if (pageNumber < 0)
            throw new InvalidPageNumberException();
        User performingUser = authenticationService.getCurrentUser();
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
        if (!event.isUserAttending(performingUser))
            throw new NotEventAttenderException();

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                paginationProperties.getDefaultPageSize(),
                Sort.by("uploadDateTime").ascending().and(Sort.by("id").ascending())
        );

        Page<FileOverviewProjection> filePage = fileRepository.findOverviewsByEventId(eventId, pageRequest);

        return FileOverviewPageDto.fromProjections(filePage);
    }

    @Transactional
    public FileOverviewDto uploadFileToEvent(FileUploadDto fileUploadDto, UUID eventId) throws IOException {

        Event event = eventRepository.findByIdForUpdate(eventId)
                .or(() -> eventRepository.findById(eventId))
                .orElseThrow(EventNotFoundException::new);
        User uploadingUser = authenticationService.getCurrentUser();

        if (!event.isUserAttending(uploadingUser))
            throw new NotEventAttenderException();

        if (fileUploadDto.getFile().isEmpty())
            throw new EmptyUploadedFileException();

        long newFileSize = fileUploadDto.getFile().getSize();
        if (newFileSize > communityProperties.getMaxFileSize().toBytes()
                || fileRepository.countByEventId(eventId) >= communityProperties.getMaxFilesPerEvent()
                || fileRepository.totalContentBytesByEventId(eventId) + newFileSize
                > communityProperties.getMaxEventFileStorage().toBytes()) {
            throw new EventFileQuotaExceededException();
        }

        byte[] content = fileUploadDto.getFile().getBytes();
        String validatedContentType = fileUtils.detectValidatedContentType(
                        fileUploadDto.getFile().getOriginalFilename(), content)
                .orElseThrow(FileTypeNotAllowedException::new);
        Instant uploadDateTime = clock.instant().truncatedTo(ChronoUnit.MINUTES);

        File fileToSave = File.builder()
                .owner(uploadingUser)
                .event(event)
                .userFileName(fileUploadDto.getUserFilename())
                .originalFileName(sanitizeOriginalFilename(fileUploadDto.getFile().getOriginalFilename()))
                .contentType(validatedContentType)
                .content(content)
                .uploadDateTime(uploadDateTime)
                .build();

        File savedFile = fileRepository.save(fileToSave);

        List<UUID> recipientIds = new ArrayList<>(event.getAttendingUsers().stream().map(User::getId).toList());
        recipientIds.add(event.getOwner().getId());
        recipientIds.removeIf(userId -> userId.equals(uploadingUser.getId()));

        notificationCommandService.notifyNewEventFile(
                eventId,
                savedFile.getId(),
                recipientIds,
                uploadingUser.getFullName()
        );

        return new FileOverviewDto(savedFile);
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload";
        }
        String sanitized = originalFilename
                .replaceAll("[\\p{Cntrl}/\\\\]", "_")
                .trim();
        return sanitized.length() <= communityProperties.getMaxDisplayFilenameLength()
                ? sanitized
                : sanitized.substring(0, communityProperties.getMaxDisplayFilenameLength());
    }
}
