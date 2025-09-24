package com.example.shoozy_shop.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class CreateReturnRequestRequest {
    private Long orderId;
    private String reason;
    private String note;
    private List<ReturnItemRequest> items;
    private RefundInfoRequest refundInfo;
}
