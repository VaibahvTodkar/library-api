package com.school.library.dto.response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

public record PageResponse<T>(

        List<T> content,

        int page,

        int size,

        long totalElements,

        int totalPages,

        boolean first,

        boolean last,

        String sort

) {

    public static <T> PageResponse<T> from(
            Page<T> page
    ) {

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                buildSort(page.getSort())
        );
    }

    private static String buildSort(
            Sort sort
    ) {

        if (sort.isUnsorted()) {
            return null;
        }

        return sort.stream()
                .map(order ->
                        order.getProperty()
                                + ","
                                + order.getDirection()
                                        .name()
                                        .toLowerCase()
                )
                .reduce(
                        (first, second) ->
                                first + "," + second
                )
                .orElse(null);
    }
}