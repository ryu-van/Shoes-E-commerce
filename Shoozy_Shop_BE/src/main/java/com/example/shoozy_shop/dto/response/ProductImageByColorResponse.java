package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ProductImageByColorResponse {
    private Long imageId;
    private String imageUrl;
}