package com.example.shoozy_shop.model;

import java.time.LocalDateTime;
import java.time.LocalDate;

public enum DefineStatus {
    UPCOMING(0),
    LAUNCHING(1),
    EXPIRED(2),
    DELETED(3),
    EXHAUSTED(4);

    private final int value;

    DefineStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DefineStatus defineStatus(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDate today = LocalDate.now();

        // Chuyển LocalDateTime về LocalDate để so sánh ngày
        LocalDate start = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();


        if (start.isAfter(today)) {
            return UPCOMING;
        } else if (end.isBefore(today)) {
            return EXPIRED;
        } else {
            return LAUNCHING;
        }
    }
    public static DefineStatus defineStatusSmartForCoupon(LocalDateTime start, LocalDateTime end, Integer quantity) {
        LocalDateTime now = LocalDateTime.now();
        if (quantity != null && quantity <= 0) return DefineStatus.EXHAUSTED;
        if (start != null && start.isAfter(now)) return DefineStatus.UPCOMING;
        if (end != null && end.isBefore(now)) return DefineStatus.EXPIRED;
        return DefineStatus.LAUNCHING;
    }

}
