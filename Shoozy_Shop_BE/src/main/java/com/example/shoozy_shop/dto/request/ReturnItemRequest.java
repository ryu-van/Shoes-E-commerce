package com.example.shoozy_shop.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class ReturnItemRequest {
    private Long orderDetailId;
    private int quantity;
    private String note;
    private List<String> imageUrls; // URL ảnh từ frontend
}
