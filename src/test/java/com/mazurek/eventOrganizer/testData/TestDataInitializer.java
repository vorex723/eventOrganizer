package com.mazurek.eventOrganizer.testData;

import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.file.FileService;
import com.mazurek.eventOrganizer.file.FileUploadDto;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadReplyCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyService;
import com.mazurek.eventOrganizer.thread.ThreadService;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyCreateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.mazurek.eventOrganizer.testData.TestConstants.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

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

    private UUID setupThreadReplyInThread(UUID eventId, UUID threadId, String replyContent, Runnable setupSecurityContextAction){
        ThreadReplyCreateDto threadReplyCreateDto = ThreadReplyCreateDtoTestBuilder.firstReply()
                .replyContent(replyContent)
                .build();
        setupSecurityContextAction.run();
        UUID threadReplyId = threadReplyService.createReplyInThread(threadReplyCreateDto, eventId, threadId).getId();
        SecurityContextHolder.clearContext();
        return threadReplyId;
    }

    public UUID setupThreadReplyInThreadByFirstUser(UUID eventId,UUID threadId){
        return setupThreadReplyInThreadByFirstUser(eventId, threadId, ThreadReplyConstants.FIRST_REPLY_CONTENT);
    }

    public UUID setupThreadReplyInThreadByFirstUser(UUID eventId, UUID threadId, String replyContent){
        return setupThreadReplyInThread(eventId, threadId, replyContent, authHelper::setupSecurityContextForFirstUser);
    }

    public List<UUID> setupThreadRepliesInThreadByFirstUser(UUID eventId, UUID threadId, int replyAmount){
        return IntStream.range(0, replyAmount)
                .mapToObj(replyNumber -> setupThreadReplyInThreadByFirstUser(
                        eventId,
                        threadId,
                        ThreadReplyConstants.FIRST_REPLY_CONTENT + " " + replyNumber))
                .toList();
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
        return setupThreadReplyInThreadBySecondUser(eventId, threadId, ThreadReplyConstants.FIRST_REPLY_CONTENT);
    }

    public UUID setupThreadReplyInThreadBySecondUser(UUID eventId, UUID threadId, String replyContent){
        return setupThreadReplyInThread(eventId, threadId, replyContent, authHelper::setupSecurityContextForSecondUser);
    }

    public List<UUID> setupThreadRepliesInThreadBySecondUser(UUID eventId, UUID threadId, int replyAmount){
        return IntStream.range(0, replyAmount)
                .mapToObj(replyNumber -> setupThreadReplyInThreadBySecondUser(
                        eventId,
                        threadId,
                        ThreadReplyConstants.FIRST_REPLY_CONTENT + " " + replyNumber))
                .toList();
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
