package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadShortDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import java.util.HashSet;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ThreadMapper {
    ThreadDto mapThreadToThreadDto(Thread thread);
    ThreadShortDto mapThreadToThreadShortDto(Thread thread);

    default int repliesToRepliesAmount(HashSet<ThreadReply> replies){
        return replies.size();
    }
}
