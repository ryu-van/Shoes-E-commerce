package com.example.shoozy_shop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CouponException extends RuntimeException {
    public CouponException(String message) {
        super(message);
    }


}
