package com.example.shoozy_shop.model;

import com.example.shoozy_shop.enums.RefundStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "order_details")

public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_variant_id", referencedColumnName = "id")
    private ProductVariant productVariant;

    private Double price;

    private Integer quantity;

    @Column(name = "total_money")
    private Double totalMoney;

    @Column(name = "promotion_code")
    private String promotionCode;

    @Column(name = "promotion_name")
    private String promotionName;

    @Column(name = "promotion_value")
    private Double promotionValue;

    @Column(name = "promotion_discount_amount")
    private Double promotionDiscountAmount;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 32, nullable = false)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "status", insertable = false)
    private Boolean status;

}
