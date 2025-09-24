// com/example/shoozy_shop/dto/request/RefundInfoRequest.java
package com.example.shoozy_shop.dto.request;

import com.example.shoozy_shop.enums.RefundMethod;
import lombok.Data;

@Data
public class RefundInfoRequest {
    private RefundMethod method; // REQUIRED
    private String bankName; // required if BANK_TRANSFER
    private String accountNumber; // required if BANK_TRANSFER
    private String accountHolder; // required if BANK_TRANSFER
    private String walletProvider; // required if EWALLET
    private String walletAccount; // required if EWALLET
}
