package com.mazurek.eventOrganizer.testData;

import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.file.FileService;
import com.mazurek.eventOrganizer.file.FileUploadDto;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadReplyCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.ThreadReplyService;
import com.mazurek.eventOrganizer.thread.ThreadService;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.mazurek.eventOrganizer.testData.TestConstants.*;

import java.io.IOException;
import java.util.UUID;

@Component
public class TestDataInitializer {

    @Autowired
    private EventService eventService;
    @Autowired
    private ThreadService threadService;
    @Autowired
    private ThreadReplyService threadReplyService;
    @Autowired
    private FileService fileService;
    @Autowired
    private AuthHelper authHelper;


    public UUID setupFirstEvent(){
        EventCreateDto eventCreateDto = EventCreateDtoTestBuilder.firstEvent().build();
        authHelper.setupSecurityContextForFirstUser();
        UUID firstEventId = eventService.createEvent(eventCreateDto).getId();
        SecurityContextHolder.clearContext();
        return firstEventId;
    }

    public UUID setupFileInEvent(UUID eventId) throws IOException {
        FileUploadDto fileUploadDto = new FileUploadDto(
                FileConstants.USER_FILE_NAME,
                MultipartFileTestBuilder.jpgFile().buildMultipartFile()
        );
        authHelper.setupSecurityContextForFirstUser();
        UUID fileId = fileService.uploadFileToEvent(fileUploadDto, eventId).getId();
        SecurityContextHolder.clearContext();
        return fileId;
    }

    public UUID setupEventByFirstUser(){
        EventCreateDto eventCreateDto = EventCreateDtoTestBuilder.secondEvent().build();
        authHelper.setupSecurityContextForFirstUser();
        UUID eventId = eventService.createEvent(eventCreateDto).getId();
        SecurityContextHolder.clearContext();
        return eventId;
    }

    public UUID setupThreadInEventByFirstUser(UUID eventId){
        ThreadCreateDto threadCreateDto = ThreadCreateDtoTestBuilder.firstThread().build();
        authHelper.setupSecurityContextForFirstUser();
        UUID threadId = threadService.createThreadInEvent(threadCreateDto, eventId).getId();
        SecurityContextHolder.clearContext();
        return threadId;
    }

    public UUID setupThreadReplyInThreadByFirstUser(UUID eventId,UUID threadId){
        ThreadReplyCreateDto threadReplyCreateDto = ThreadReplyCreateDtoTestBuilder.firstReply().build();
        authHelper.setupSecurityContextForFirstUser();
        UUID ThreadReplyId = threadReplyService.createReplyInThread(threadReplyCreateDto, eventId, threadId).getId();
        SecurityContextHolder.clearContext();
        return ThreadReplyId;
    }
    public UUID setupEventBySecondUser(){
        EventCreateDto eventCreateDto = EventCreateDtoTestBuilder.secondEvent().build();
        authHelper.setupSecurityContextForSecondUser();
        UUID eventId = eventService.createEvent(eventCreateDto).getId();
        SecurityContextHolder.clearContext();
        return eventId;
    }

    public UUID setupThreadInEventBySecondUser(UUID eventId){
        ThreadCreateDto threadCreateDto = ThreadCreateDtoTestBuilder.firstThread().build();
        authHelper.setupSecurityContextForSecondUser();
        UUID threadId = threadService.createThreadInEvent(threadCreateDto, eventId).getId();
        SecurityContextHolder.clearContext();
        return threadId;
    }

    public UUID setupThreadReplyInThreadBySecondUser(UUID eventId,UUID threadId){
        ThreadReplyCreateDto threadReplyCreateDto = ThreadReplyCreateDtoTestBuilder.firstReply().build();
        authHelper.setupSecurityContextForSecondUser();
        UUID ThreadReplyId = threadReplyService.createReplyInThread(threadReplyCreateDto, eventId, threadId).getId();
        SecurityContextHolder.clearContext();
        return ThreadReplyId;
    }

    public void addFirstUserToAttenders(UUID eventId){
        authHelper.setupSecurityContextForFirstUser();
        eventService.addAttenderToEvent(eventId);
        SecurityContextHolder.clearContext();
    }
    public void addSecondUserToAttenders(UUID eventId){
        authHelper.setupSecurityContextForSecondUser();
        eventService.addAttenderToEvent(eventId);
        SecurityContextHolder.clearContext();
    }
}
