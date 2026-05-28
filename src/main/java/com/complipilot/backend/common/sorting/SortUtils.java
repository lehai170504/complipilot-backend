package com.complipilot.backend.common.sorting;

import java.util.Map;

import com.complipilot.backend.common.error.BadRequestException;

import org.springframework.data.domain.Sort;

public final class SortUtils {

    private SortUtils() {
    }

    public static Sort toSort(
            SortRequest request,
            Map<String, String> allowedFields,
            String defaultField
    ) {
        String requestedSortBy = request == null ? null : request.sortBy();
        SortDirection requestedDirection = request == null ? null : request.sortDirection();

        String sortBy = requestedSortBy == null || requestedSortBy.isBlank()
                ? defaultField
                : requestedSortBy.trim();

        String entityField = allowedFields.get(sortBy);

        if (entityField == null) {
            throw new BadRequestException("Unsupported sort field: " + sortBy);
        }

        Sort.Direction direction = requestedDirection == SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, entityField);
    }
}