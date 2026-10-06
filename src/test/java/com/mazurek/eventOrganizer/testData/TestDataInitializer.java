package com.mazurek.eventOrganizer.testData;

import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.file.FileService;
import com.mazurek.eventOrganizer.file.FileUploadDto;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.FileUploadDtoTestBuilder;
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
        try {
            EventCreateDto eventCreateDto = EventCreateDtoTestBuilder.firstEvent().build();
            authHelper.setupSecurityContextForFirstUser();
            UUID firstEventId = eventService.createEvent(eventCreateDto).getId();
            return firstEventId;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public UUID setupFileInEvent(UUID eventId) throws IOException {
        try {
            FileUploadDto fileUploadDto = new FileUploadDtoTestBuilder().build();
            authHelper.setupSecurityContextForFirstUser();
            UUID fileId = fileService.uploadFileToEvent(fileUploadDto, eventId).getId();
            return fileId;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public UUID setupEventByFirstUser(){
        try {
            EventCreateDto eventCreateDto = EventCreateDtoTestBuilder.secondEvent().build();
            authHelper.setupSecurityContextForFirstUser();
            UUID eventId = eventService.createEvent(eventCreateDto).getId();
            return eventId;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public UUID setupThreadInEventByFirstUser(UUID eventId){
        try {
            ThreadCreateDto threadCreateDto = ThreadCreateDtoTestBuilder.firstThread().build();
            authHelper.setupSecurityContextForFirstUser();
            UUID threadId = threadService.createThreadInEvent(threadCreateDto, eventId).getId();
            return threadId;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private UUID setupThreadReplyInThread(UUID eventId, UUID threadId, String replyContent, Runnable setupSecurityContextAction){
        try {
            ThreadReplyCreateDto threadReplyCreateDto = ThreadReplyCreateDtoTestBuilder.firstReply()
                    .replyContent(replyContent)
                    .build();
            setupSecurityContextAction.run();
            UUID threadReplyId = threadReplyService.createReplyInThread(threadReplyCreateDto, eventId, threadId).getId();
            return threadReplyId;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public UUID setupThreadReplyInThreadByFirstUser(UUID eventId,UUID threadId){
        return setupThreadReplyInThreadByFirstUser(eventId, threadId, ThreadReplyConstants.FIRST_REPLY_CONTENT);
    }

    public UUID setupThreadReplyInThreadByFirstUser(UUID eventId, UUID threadId, String replyContent){
        return setupThreadReplyInThread(eventId, threadId, replyContent, authHelper::setupSecurityContextForFirstUser);
    }

    public List<UUID> setupThreadRepliesInThreadByFirstUser(UUID eventId, UUID threadId, int replyAmount){
        try {
            return IntStream.range(0, replyAmount)
                    .mapToObj(replyNumber -> setupThreadReplyInThreadByFirstUser(
                            eventId,
                            threadId,
                            ThreadReplyConstants.FIRST_REPLY_CONTENT + " " + replyNumber))
                    .toList();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public UUID setupEventBySecondUser(){
        try {
            EventCreateDto eventCreateDto = EventCreateDtoTestBuilder.secondEvent().build();
            authHelper.setupSecurityContextForSecondUser();
            UUID eventId = eventService.createEvent(eventCreateDto).getId();
            return eventId;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public UUID setupThreadInEventBySecondUser(UUID eventId){
        try {
            ThreadCreateDto threadCreateDto = ThreadCreateDtoTestBuilder.firstThread().build();
            authHelper.setupSecurityContextForSecondUser();
            UUID threadId = threadService.createThreadInEvent(threadCreateDto, eventId).getId();
            return threadId;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public UUID setupThreadReplyInThreadBySecondUser(UUID eventId,UUID threadId){
        return setupThreadReplyInThreadBySecondUser(eventId, threadId, ThreadReplyConstants.FIRST_REPLY_CONTENT);
    }

    public UUID setupThreadReplyInThreadBySecondUser(UUID eventId, UUID threadId, String replyContent){
        return setupThreadReplyInThread(eventId, threadId, replyContent, authHelper::setupSecurityContextForSecondUser);
    }

    public List<UUID> setupThreadRepliesInThreadBySecondUser(UUID eventId, UUID threadId, int replyAmount){
        try {
            return IntStream.range(0, replyAmount)
                    .mapToObj(replyNumber -> setupThreadReplyInThreadBySecondUser(
                            eventId,
                            threadId,
                            ThreadReplyConstants.FIRST_REPLY_CONTENT + " " + replyNumber))
                    .toList();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public void addFirstUserToAttendees(UUID eventId){
        try {
            authHelper.setupSecurityContextForFirstUser();
            eventService.addAttendeeToEvent(eventId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    public void addSecondUserToAttendees(UUID eventId){
        try {
            authHelper.setupSecurityContextForSecondUser();
            eventService.addAttendeeToEvent(eventId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
