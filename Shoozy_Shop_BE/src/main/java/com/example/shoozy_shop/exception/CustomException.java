package com.example.shoozy_shop.exception;

public class CustomException extends RuntimeException {
    private final int status;

    public CustomException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}

