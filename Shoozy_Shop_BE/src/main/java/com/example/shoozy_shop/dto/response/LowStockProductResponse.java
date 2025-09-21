package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LowStockProductResponse {
    private Long productId;
    private String productName;
    private String thumbnail;
    private Long totalQuantity;
}