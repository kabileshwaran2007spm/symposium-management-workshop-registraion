package com.campusconnect.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Page payload returned inside {@link ApiResponse#data()}.
 *
 * <p>The planning documents specify a Pagination component but never define the
 * response shape for a paginated list, so this is the explicit contract: list
 * endpoints return this object rather than a bare array.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
