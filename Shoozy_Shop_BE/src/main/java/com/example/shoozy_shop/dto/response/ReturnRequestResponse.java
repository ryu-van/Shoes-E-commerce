package com.example.shoozy_shop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class ReturnRequestResponse {
    private Long id;
    private String reason;
    private String note;
    private String status;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private OrderSummaryDto order;
    private List<ReturnItemResponse> returnItems;
    private RefundTransactionDto refundTransaction;
    private RefundInfoDto refundInfo;
}
