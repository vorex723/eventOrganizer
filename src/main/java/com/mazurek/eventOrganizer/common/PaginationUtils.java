package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static void requireValidPageNumber(int pageNumber) {
        if (pageNumber < 0) {
            throw new InvalidPageNumberException();
        }
    }

    public static PageRequest pageRequest(int pageNumber, int pageSize, Sort sort) {
        requireValidPageNumber(pageNumber);
        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
