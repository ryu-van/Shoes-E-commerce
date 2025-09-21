package com.example.shoozy_shop.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class TransactionRequest {

    @JsonProperty("Order_Id")
    private Long OrderId;

    private Double amount;

    private String note;

    private String vnpTxnRef;

    private LocalDateTime transactionDate;

    private LocalDateTime completedDate;


}
