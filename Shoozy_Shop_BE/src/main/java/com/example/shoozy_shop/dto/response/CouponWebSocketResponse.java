package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponWebSocketResponse {
    private String type;
    private Long couponId;
    private String code;
    private String name;
    private Integer quantity;
    private Integer status;
    private String action;
    private LocalDateTime timestamp;
    private String message;
    
    // Thông tin đơn hàng nếu có
    private Long orderId;
    private String orderCode;
    private String customerName;
    
    public static CouponWebSocketResponse fromCouponUpdate(Long couponId, String code, String name, 
                                                         Integer quantity, Integer status, String action) {
        return CouponWebSocketResponse.builder()
                .type("COUPON_UPDATE")
                .couponId(couponId)
                .code(code)
                .name(name)
                .quantity(quantity)
                .status(status)
                .action(action)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static CouponWebSocketResponse fromOrderWithCoupon(Long couponId, String code, String name,
                                                            Integer quantity, Integer status, Long orderId, 
                                                            String orderCode, String customerName) {
        return CouponWebSocketResponse.builder()
                .type("ORDER_WITH_COUPON")
                .couponId(couponId)
                .code(code)
                .name(name)
                .quantity(quantity)
                .status(status)
                .action("COUPON_USED")
                .timestamp(LocalDateTime.now())
                .orderId(orderId)
                .orderCode(orderCode)
                .customerName(customerName)
                .message("Coupon " + code + " đã được sử dụng trong đơn hàng " + orderCode)
                .build();
    }
}

