package com.campusconnect.dto;

import org.springframework.data.domain.Sort;

public enum EventSort {
    UPCOMING_DATE(Sort.by(Sort.Order.asc("date"), Sort.Order.asc("startTime"))),
    LATEST_DATE(Sort.by(Sort.Order.desc("date"), Sort.Order.asc("startTime"))),
    TITLE(Sort.by(Sort.Order.asc("title"), Sort.Order.asc("date")));

    private final Sort sort;

    EventSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
