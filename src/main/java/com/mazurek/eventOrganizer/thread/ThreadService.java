package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.common.SortDirection;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewPageDto;

import java.util.UUID;

public interface ThreadService {
    ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId);
    ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId, UUID threadId);
    ThreadOverviewPageDto getThreadsByEventId(UUID eventId, int pageNumber, ThreadSortField sortByField, SortDirection direction);
    ThreadDto getThreadInEvent(UUID eventId, UUID threadId);
}
