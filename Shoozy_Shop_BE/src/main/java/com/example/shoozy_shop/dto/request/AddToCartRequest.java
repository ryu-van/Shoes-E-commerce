package com.example.shoozy_shop.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToCartRequest {
    private Long userId;
    private Long productVariantId;
    private Integer quantity;
}
