package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.model.Coupon;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {

    private Long id;
    private String name;
    private String code;
    private Boolean type;
    private String description;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    private Double value;
    private Double condition;
    private Integer status;
    private Integer quantity;

    @Column(name = "value_limit")
    private Double valueLimit;


    public static CouponResponse convertToResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .startDate(coupon.getStartDate())
                .expirationDate(coupon.getExpirationDate())
                .value(coupon.getValue())
                .condition(coupon.getCondition())
                .status(coupon.getStatus())
                .quantity(coupon.getQuantity())
                .type(coupon.getType())
                .code(coupon.getCode())
                .valueLimit(coupon.getValueLimit())
                .build();
    }
}
