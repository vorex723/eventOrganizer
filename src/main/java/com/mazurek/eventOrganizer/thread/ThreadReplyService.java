package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyDto;

import java.util.UUID;

public interface ThreadReplyService {

    ThreadReplyDto createReplyInThread(ThreadReplyCreateDto threadReplyCreateDto, UUID eventId, UUID threadId);
    ThreadReplyDto updateThreadReplyInEventThread(ThreadReplyCreateDto threadReplyUpdateDto, UUID eventId, UUID threadId, UUID threadReplyId);
}
