package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {
    private String productName;
    private Integer size;
    private String color;
    private String material;
    private String thumbnail;
    private Integer quantity;
    private Double price;
    private Double totalMoney;
}
