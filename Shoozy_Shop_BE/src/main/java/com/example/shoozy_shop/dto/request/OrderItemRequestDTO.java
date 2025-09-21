package com.example.shoozy_shop.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequestDTO {
    private Long productVariantId; // ID biến thể sản phẩm
    private Integer quantity; // Số lượng
    private Double price; // Đơn giá
}
