package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDTO {
    private Long orderId;
    private String orderCode;
    private LocalDateTime orderDate;
    private String status;
    private String customerName;
    private String phoneNumber;
    private String email;
    private String shippingAddress;
    private String note;
    private List<OrderItemResponseDTO> items;
    private String couponName;
    private String couponDescription;
    private Double couponValue;
    private Double totalItems; // Tổng tiền hàng (trước giảm giá)
    private BigDecimal totalMoney; // Tổng tiền thanh toán (sau giảm giá)
    private String paymentMethod;
    private String shippingMethod;
    private LocalDateTime shippingDate;
    private String shopName;
    private String shopTaxCode;
    private String shopAddress;
    private String shopPhone;
}
