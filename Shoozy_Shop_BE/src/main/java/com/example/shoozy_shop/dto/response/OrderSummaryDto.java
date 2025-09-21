package com.example.shoozy_shop.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSummaryDto {
    private Long id;
    private String orderCode;
    private String fullname;
    private String phoneNumber;
    private String shippingAddress;
    private BigDecimal totalMoney;
}

