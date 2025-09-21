package com.example.shoozy_shop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // vẫn OK; hoặc bỏ nếu đã có GlobalExceptionHandle
public class DuplicateResourceException extends RuntimeException {
    private final String code; // "EMAIL_EXISTS" | "PHONE_EXISTS" | ...

    public DuplicateResourceException(String code, String message) {
        super(message);
        this.code = code;
    }

    // Giữ constructor cũ để tương thích (nếu nơi khác vẫn gọi new
    // DuplicateResourceException("..."))
    public DuplicateResourceException(String message) {
        super(message);
        this.code = "DUPLICATE";
    }

    public String getCode() {
        return code;
    }
}
