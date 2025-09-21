package com.example.shoozy_shop.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {
    private Long productId;
    private String content;
    private Integer rating;
    private Long orderDetailId;
} 