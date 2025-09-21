package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.model.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionCode;
    private BigDecimal amount;
    private String status;
    private LocalDateTime transactionDate;
    private LocalDateTime completedDate;
    private String note;
    private VnPayTransactionDetail vnPayTransactionDetail;

    public static TransactionResponse fromEntity(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionCode(transaction.getTransactionCode())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .transactionDate(transaction.getTransactionDate())
                .completedDate(transaction.getCompletedDate())
                .note(transaction.getNote())
                .vnPayTransactionDetail(transaction.getVnPayTransactionDetail())
                .build();
    }
}
