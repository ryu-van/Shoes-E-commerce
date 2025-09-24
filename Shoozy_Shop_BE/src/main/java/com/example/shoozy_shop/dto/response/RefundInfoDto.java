// com/example/shoozy_shop/dto/response/RefundInfoDto.java
package com.example.shoozy_shop.dto.response;

import lombok.Data;

@Data
public class RefundInfoDto {
    private String method;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String walletProvider;
    private String walletAccount;
}
