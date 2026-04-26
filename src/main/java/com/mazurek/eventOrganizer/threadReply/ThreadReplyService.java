package com.mazurek.eventOrganizer.threadReply;

import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyPageDto;

import java.util.UUID;

public interface ThreadReplyService {

    ThreadReplyDto createReplyInThread(ThreadReplyCreateDto threadReplyCreateDto, UUID eventId, UUID threadId);
    ThreadReplyDto updateThreadReplyInEventThread(ThreadReplyCreateDto threadReplyUpdateDto, UUID eventId, UUID threadId, UUID threadReplyId);
    ThreadReplyPageDto getRepliesInEventThread(UUID eventId, UUID threadId, int pageNumber);
}
