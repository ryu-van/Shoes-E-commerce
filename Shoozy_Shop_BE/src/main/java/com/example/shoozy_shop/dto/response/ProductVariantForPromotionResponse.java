package com.example.shoozy_shop.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariantForPromotionResponse {
    private Long id;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer quantity;
}
