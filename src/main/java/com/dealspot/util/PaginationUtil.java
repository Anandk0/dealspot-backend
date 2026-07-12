package com.dealspot.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtil {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    public static Pageable createPageable(int page, int size) {
        return createPageable(page, size, Sort.unsorted());
    }

    public static Pageable createPageable(int page, int size, Sort sort) {
        int validSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int validPage = Math.max(page, 0);
        return PageRequest.of(validPage, validSize, sort);
    }
}
