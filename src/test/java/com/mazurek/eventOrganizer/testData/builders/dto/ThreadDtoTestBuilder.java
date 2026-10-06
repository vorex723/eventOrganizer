package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.testData.builders.ThreadTestBuilder;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.ThreadConstants.FIRST_THREAD_ID;

/** Fresh service-return fixture; defaults reuse the corresponding domain builder. */
public class ThreadDtoTestBuilder {
    private UUID id = FIRST_THREAD_ID;

    public ThreadDtoTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public ThreadDto build() {
        return new ThreadDto(ThreadTestBuilder.firstThread().id(id).build());
    }
}
