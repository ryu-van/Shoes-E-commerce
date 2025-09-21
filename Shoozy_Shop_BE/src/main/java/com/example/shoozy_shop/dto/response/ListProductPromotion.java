package com.example.shoozy_shop.dto.response;

import lombok.*;

import java.util.List;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ListProductPromotion {
    private List<ProductPromotionResponse> productPromotions;
    private int totalPage;
    private int totalElements;
}
