package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.model.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderCode;
    private Boolean type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String status;
    private BigDecimal totalMoney;
    private LocalDateTime shippingDate;
    private BigDecimal shippingFee;
    private BigDecimal couponDiscountAmount;
    private BigDecimal finalPrice;
    private Boolean active;
    private User user;
    private PaymentMethod paymentMethod;
    private Coupon coupon;
    private String note;
    private List<OrderDetailResponse> orderDetails;
    private List<TransactionResponse> transactions;

    public static OrderResponse fromEntity(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .type(order.getType())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .fullName(order.getFullname())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .status(order.getStatus())
                .totalMoney(order.getTotalMoney())
                .shippingFee(order.getShippingFee())
                .finalPrice(order.getFinalPrice())
                .couponDiscountAmount(order.getCouponDiscountAmount())
                .active(order.getActive())
                .user(order.getUser())
                .paymentMethod(order.getPaymentMethod())
                .coupon(order.getCoupon())
                .note(order.getNote())
                .orderDetails(order.getOrderDetails() != null ?
                        order.getOrderDetails().stream()
                                .map(OrderDetailResponse::fromEntity)
                                .toList() : null)
                .transactions(order.getTransactions() != null ?
                        order.getTransactions().stream()
                                .map(TransactionResponse::fromEntity)
                                .toList() : null)
                .build();
    }
}