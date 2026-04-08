package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.notification.NotificationService;
import com.mazurek.eventOrganizer.notification.NotificationType;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final int PAGE_DEFAULT_SIZE = 20;

    private final AuthenticationService authenticationService;
    private final NotificationService notificationService;
    private final EventRepository eventRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileUtils fileUtils;

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

        PageRequest pageRequest = PageRequest.of(pageNumber, PAGE_DEFAULT_SIZE, Sort.by("uploadDateTime").ascending());

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

        if (!fileUtils.isFileCorrect(fileUploadDto.getFile()))
            throw new FileTypeNotAllowedException();
        Instant uploadDateTime = Instant.now().truncatedTo(ChronoUnit.MINUTES);

        File fileToSave = File.builder()
                .owner(user)
                .event(event)
                .userFileName(fileUploadDto.getUserFilename())
                .originalFileName(fileUploadDto.getFile().getOriginalFilename())
                .contentType(fileUploadDto.getFile().getContentType())
                .content(fileUploadDto.getFile().getBytes())
                .uploadDateTime(uploadDateTime)
                .build();

        event.addFile(fileToSave);
        user.addFile(fileToSave);
        File savedFile = fileRepository.save(fileToSave);

        eventRepository.save(event);
        userRepository.save(user);

        notificationService.notifyEventAttenders(event, NotificationType.EVENT_NEW_FILE, eventId, user.getFullName());

        return new FileOverviewDto(savedFile);
    }
}
