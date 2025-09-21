package com.example.shoozy_shop.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class VariantPromotionResponse {
    private Long id;
    private String name;
    private String code;
    private LocalDateTime startDate;
    private LocalDateTime expirationDate;
    private Double originalValue; // Value gốc từ bảng promotions
    private Double customValue;   // Custom value từ bảng product_promotions
    private String description;
    private Integer status;
}