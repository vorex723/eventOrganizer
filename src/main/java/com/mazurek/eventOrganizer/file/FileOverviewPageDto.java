package com.mazurek.eventOrganizer.file;

import org.springframework.data.domain.Page;
import java.util.List;

public record FileOverviewPageDto(
        List<FileOverviewDto> fileOverviews,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean lastPage
) {

    public FileOverviewPageDto(Page<File> filePage) {

        this(
                filePage.getContent().stream().map(FileOverviewDto::new).toList(),
                filePage.getNumber(),
                filePage.getSize(),
                filePage.getTotalElements(),
                filePage.getTotalPages(),
                filePage.isLast()
                );
    }
}

