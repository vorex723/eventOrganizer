package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class ThreadCreateDtoTestBuilder {

    private String name = ThreadConstants.FIRST_THREAD_NAME;
    private String content = ThreadConstants.FIRST_THREAD_CONTENT;

    public static ThreadCreateDtoTestBuilder firstThread() {
        return new ThreadCreateDtoTestBuilder();
    }

    public static ThreadCreateDtoTestBuilder firstThreadUpdate() {
        return new ThreadCreateDtoTestBuilder().name(ThreadConstants.FIRST_THREAD_NAME_UPDATE).content(ThreadConstants.FIRST_THREAD_CONTENT_UPDATE);
    }

    public ThreadCreateDtoTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ThreadCreateDtoTestBuilder content(String content) {
        this.content = content;
        return this;
    }

    public ThreadCreateDto build() {
        return ThreadCreateDto.builder()
                .name(name)
                .content(content)
                .build();
    }

}
