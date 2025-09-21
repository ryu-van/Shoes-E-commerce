package com.example.shoozy_shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "vnpay_transaction_details")
public class VnPayTransactionDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "transaction_id")
    @JsonIgnore
    private Transaction transaction;

    @Column(name = "vnp_txn_ref", nullable = false, length = 100)
    private String vnpTxnRef;

    @Column(name = "vnp_transaction_no", length = 100)
    private String vnpTransactionNo;

    @Column(name = "vnp_bank_code", length = 20)
    private String vnpBankCode;

    @Column(name = "vnp_card_type", length = 20)
    private String vnpCardType;

    @Column(name = "vnp_pay_date", length = 14)
    private String vnpPayDate;

    @Column(name = "vnp_order_info")
    private String vnpOrderInfo;

    @Column(name = "vnp_response_code", length = 2)
    private String vnpResponseCode;

    @Column(name = "vnp_transaction_status", length = 2)
    private String vnpTransactionStatus;

    @Column(name = "request_url", columnDefinition = "NVARCHAR(MAX)")
    private String requestUrl;

    @Column(name = "ipn_data", columnDefinition = "NVARCHAR(MAX)")
    private String ipnData;

    @Column(name = "return_data", columnDefinition = "NVARCHAR(MAX)")
    private String returnData;

    @Column(name = "secure_hash", length = 256)
    private String secureHash;

}
