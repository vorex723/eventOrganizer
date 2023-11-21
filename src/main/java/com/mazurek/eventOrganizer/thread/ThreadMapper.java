package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ThreadMapper {
    ThreadDto mapThreadToThreadDto(Thread thread);
}
