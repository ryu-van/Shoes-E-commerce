package com.example.shoozy_shop.dto.response;


import com.example.shoozy_shop.model.Product;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProductResponseForPromotion {
    private Long id;
    private String name;
    private Integer quantity;

    private List<ProductVariantForPromotionResponse> variants;
    ;
}
