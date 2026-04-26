package com.mazurek.eventOrganizer.threadReply.dto;

import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import org.springframework.data.domain.Page;

import java.util.List;

public record ThreadReplyPageDto(
        List<ThreadReplyDto> replies,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean lastPage
) {
    public ThreadReplyPageDto(Page<ThreadReply> repliesPage){
        this(
                repliesPage.getContent().stream().map(ThreadReplyDto::new).toList(),
                repliesPage.getNumber(),
                repliesPage.getSize(),
                repliesPage.getTotalElements(),
                repliesPage.getTotalPages(),
                repliesPage.isLast()
        );
    }
}
