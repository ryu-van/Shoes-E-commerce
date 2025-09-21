package com.example.shoozy_shop.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class UpdateOrderStatusRequest {
    private Long orderId;
    private String newStatus;
    private String description;
    private Long userId;
}