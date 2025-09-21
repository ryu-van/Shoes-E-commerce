package com.example.shoozy_shop.dto.response;

import lombok.*;


@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPromotionResponse {
    private Long id;
    private String name;
    private Double price;
    private String thumbnail;
    private Long quantity;
}
