package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyCreateDto;

import static com.mazurek.eventOrganizer.testData.TestConstants.ThreadReplyConstants.FIRST_REPLY_CONTENT;
import static com.mazurek.eventOrganizer.testData.TestConstants.ThreadReplyConstants.FIRST_REPLY_UPDATE_CONTENT;

public class ThreadReplyCreateDtoTestBuilder {

    private String replyContent = FIRST_REPLY_CONTENT;

    public static ThreadReplyCreateDtoTestBuilder firstReply() {
        return new ThreadReplyCreateDtoTestBuilder();
    }
    public static ThreadReplyCreateDtoTestBuilder firstReplyUpdate() {
        return new ThreadReplyCreateDtoTestBuilder().replyContent(FIRST_REPLY_UPDATE_CONTENT);
    }

    public ThreadReplyCreateDtoTestBuilder replyContent(String replyContent) {
        this.replyContent = replyContent;
        return this;
    }

    public ThreadReplyCreateDto build() {
        return ThreadReplyCreateDto.builder()
                .replyContent(replyContent)
                .build();
    }
}
