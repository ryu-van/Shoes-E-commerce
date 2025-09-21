package com.example.shoozy_shop.dto.response;


import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListPromotionResponse {
    private List<PromotionResponse> promotions;
    private int totalPage;
    private int totalElements;
}
