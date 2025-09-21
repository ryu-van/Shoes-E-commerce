package com.example.shoozy_shop.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "orders")

public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "payment_method_id", referencedColumnName = "id")
    private PaymentMethod paymentMethod;

    private String fullname;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String address;

    private String note;

    private Boolean type;

    @ManyToOne
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "status", insertable = false)
    private String status;

    @Column(name = "total_money")
    private BigDecimal totalMoney;

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee;

    @Column(insertable = false)
    private Boolean active;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "coupon_name")
    private String couponName;

    @Column(name = "coupon_type")
    private Boolean couponType;

    @Column(name = "coupon_value")
    private BigDecimal couponValue;

    @Column(name = "coupon_value_limit")
    private BigDecimal couponValueLimit;

    @Column(name = "coupon_discount_amount")
    private BigDecimal couponDiscountAmount;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Transaction> transactions;
}