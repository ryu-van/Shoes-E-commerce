package com.example.shoozy_shop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PromotionException extends RuntimeException{
    public PromotionException(String message) {
        super(message);
    }
}
