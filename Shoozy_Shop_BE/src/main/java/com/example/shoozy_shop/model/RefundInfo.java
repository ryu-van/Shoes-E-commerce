// com/example/shoozy_shop/model/RefundInfo.java
package com.example.shoozy_shop.model;

import com.example.shoozy_shop.enums.RefundMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refund_infos")
public class RefundInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false, unique = true)
    private ReturnRequest returnRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 40, nullable = false)
    private RefundMethod method; // CASH / BANK_TRANSFER / EWALLET

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "account_holder", length = 200)
    private String accountHolder;

    @Column(name = "wallet_provider", length = 100)
    private String walletProvider;

    @Column(name = "wallet_account", length = 100)
    private String walletAccount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
