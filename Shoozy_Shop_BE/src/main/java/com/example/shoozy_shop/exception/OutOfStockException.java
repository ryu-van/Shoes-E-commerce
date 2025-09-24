package com.example.shoozy_shop.exception;

import lombok.Getter;

@Getter
public class OutOfStockException extends RuntimeException{
    private final int cartQuantity;
    private final int allowAdd;
    public OutOfStockException(int cartQuantity, int allowAdd) {
        super("OUT_OF_STOCK");
        this.cartQuantity = Math.max(0, cartQuantity);
        this.allowAdd = Math.max(0, allowAdd);
    }
}
