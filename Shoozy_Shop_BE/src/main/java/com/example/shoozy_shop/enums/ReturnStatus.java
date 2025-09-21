package com.example.shoozy_shop.enums;

public enum ReturnStatus {
    PENDING,
    APPROVED,
    REJECTED,
    WAIT_FOR_PICKUP,
    RETURNED,
    REFUNDED,
    CANCELLED,
    COMPLETED;

    public boolean canTransitionTo(ReturnStatus next) {
        return switch (this) {
            case PENDING -> next == APPROVED || next == REJECTED;
            case APPROVED -> next == WAIT_FOR_PICKUP || next == CANCELLED;
            case WAIT_FOR_PICKUP -> next == RETURNED;
            case RETURNED -> next == COMPLETED || next == REFUNDED;
            default -> false;
        };
    }
}
