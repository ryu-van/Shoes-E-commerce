package com.example.shoozy_shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @OneToOne(mappedBy = "transaction")
    @JsonIgnore
    private VnPayTransactionDetail vnPayTransactionDetail;

    @Column(name = "transaction_code", length = 50, unique = true)
    private String transactionCode;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 20)
    private String status;

    @Column(name = "vnp_txn_ref", length = 100)
    private String vnpTxnRef;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String note;

}
