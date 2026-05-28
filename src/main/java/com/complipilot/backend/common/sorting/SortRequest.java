package com.complipilot.backend.common.sorting;

public record SortRequest (
    String sortBy,
    SortDirection sortDirection
){
}
