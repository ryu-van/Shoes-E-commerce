package com.example.shoozy_shop.dto.request;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ProductPromotionRequest {

    private Long idPromotionProduct;

    private Double customValue;


    private List<Long> productVariantIds;
}
