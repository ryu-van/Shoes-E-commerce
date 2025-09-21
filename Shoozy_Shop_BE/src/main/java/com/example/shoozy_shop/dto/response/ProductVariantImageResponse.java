package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class ProductVariantImageResponse {
    private Long variantId;
    private String imageUrl;
}
