package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pagination utils unit tests:")
class PaginationUtilsUnitTest {

    @Test
    @DisplayName("When page number is negative should throw invalid page number exception")
    void whenPageNumberIsNegativeShouldThrowInvalidPageNumberException() {
        assertThatThrownBy(() -> PaginationUtils.pageRequest(-1, 20, Sort.unsorted()))
                .isInstanceOf(InvalidPageNumberException.class);
    }

    @Test
    @DisplayName("When page number is valid should preserve page size and sort")
    void whenPageNumberIsValidShouldPreservePageSizeAndSort() {
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

        PageRequest pageRequest = PaginationUtils.pageRequest(2, 20, sort);

        assertThat(pageRequest.getPageNumber()).isEqualTo(2);
        assertThat(pageRequest.getPageSize()).isEqualTo(20);
        assertThat(pageRequest.getSort()).isEqualTo(sort);
    }
}
