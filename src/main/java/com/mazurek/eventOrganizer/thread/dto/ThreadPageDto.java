package com.mazurek.eventOrganizer.thread.dto;

import java.util.List;
import org.springframework.data.domain.Page;
import com.mazurek.eventOrganizer.thread.Thread;

public record ThreadPageDto(
        List<ThreadOverviewDto> threads,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean lastPage
) {
    public ThreadPageDto(Page<Thread> threadPage){
         this(
                 threadPage.getContent().stream().map(ThreadOverviewDto::new).toList(),
                 threadPage.getNumber(),
                 threadPage.getSize(),
                 threadPage.getTotalElements(),
                 threadPage.getTotalPages(),
                 threadPage.isLast()
         );
    }
}
