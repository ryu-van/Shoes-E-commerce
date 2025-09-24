package com.example.shoozy_shop.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RefundTransactionDto {
    private BigDecimal amount;
    private String method; // CASH / BANK_TRANSFER / EWALLET
    private String referenceCode; // mã GD ngân hàng / phiếu tiền mặt / ví
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}