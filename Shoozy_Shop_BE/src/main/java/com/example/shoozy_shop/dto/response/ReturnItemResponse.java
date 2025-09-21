package com.example.shoozy_shop.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class ReturnItemResponse {
    private Long id;
    private Long orderDetailId;
    private String productName;
    private Integer quantity;
    private String note;
    private List<String> imageUrls;
}
