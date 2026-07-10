package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final AuthenticationService authenticationService;
    private final EventRepository eventRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileUtils fileUtils;
    private final PaginationProperties paginationProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FileOverviewDto getFileOverviewById(UUID fileId, UUID eventId) {
        User performingUser = authenticationService.getCurrentUser();
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (!event.isUserAttending(performingUser))
            throw new NotEventAttenderException();

        return new FileOverviewDto(fileRepository.findByIdAndEventId(fileId, eventId).orElseThrow(FileNotFoundInEventException::new));
    }

    @Transactional(readOnly = true)
    public File getFileDataById(UUID fileId, UUID eventId) {
        User performingUser = authenticationService.getCurrentUser();
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (!event.isUserAttending(performingUser))
            throw new NotEventAttenderException();

        return fileRepository.findByIdAndEventId(fileId, eventId).orElseThrow(FileNotFoundInEventException::new);
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
                Sort.by("uploadDateTime").ascending()
        );

        Page<File> filePage = fileRepository.findByEventId(eventId, pageRequest);

        return new FileOverviewPageDto(filePage);
    }

    @Transactional
    public FileOverviewDto uploadFileToEvent(FileUploadDto fileUploadDto, UUID eventId) throws IOException {

        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
        User user = authenticationService.getCurrentUser();

        if (!event.isUserAttending(user))
            throw new NotEventAttenderException();

        if (fileUploadDto.getFile().isEmpty())
            throw new EmptyUploadedFileException();

        String validatedContentType = fileUtils.detectValidatedContentType(fileUploadDto.getFile())
                .orElseThrow(FileTypeNotAllowedException::new);
        Instant uploadDateTime = clock.instant().truncatedTo(ChronoUnit.MINUTES);

        File fileToSave = File.builder()
                .owner(user)
                .event(event)
                .userFileName(fileUploadDto.getUserFilename())
                .originalFileName(fileUploadDto.getFile().getOriginalFilename())
                .contentType(validatedContentType)
                .content(fileUploadDto.getFile().getBytes())
                .uploadDateTime(uploadDateTime)
                .build();

        event.addFile(fileToSave);
        user.addFile(fileToSave);
        File savedFile = fileRepository.save(fileToSave);

        eventRepository.save(event);
        userRepository.save(user);

        return new FileOverviewDto(savedFile);
    }
}
