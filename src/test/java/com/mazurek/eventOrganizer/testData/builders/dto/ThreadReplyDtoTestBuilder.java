package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyDto;
import com.mazurek.eventOrganizer.testData.builders.ThreadReplyTestBuilder;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.ThreadReplyConstants.FIRST_REPLY_ID;

/** Fresh service-return fixture; defaults reuse the corresponding domain builder. */
public class ThreadReplyDtoTestBuilder {
    private UUID id = FIRST_REPLY_ID;

    public ThreadReplyDtoTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public ThreadReplyDto build() {
        return new ThreadReplyDto(ThreadReplyTestBuilder.firstReply().id(id).build());
    }
}
