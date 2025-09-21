package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReturnableItemDto {
    private Long orderDetailId;
    private int quantity;
    private String thumbnail;
    private String color;
    private Integer size;
    private String productName;
}
