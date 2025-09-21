package com.example.shoozy_shop.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductCheckoutResponse {
    private Long idCartItem;
    private Long idProductVariant;
    private String productName;
    private String productImage;
    private Double price;
    private String size;          // để String
    private String color;
    private Integer quantity;
    private Double weight;
    private Long idPromotion;
    private Double valuePromotion;
}

