package com.example.shoozy_shop.dto.response;

import lombok.*;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class CouponForOrderResponse {
    private Long id;
    private String name;
    private String code;
    private Boolean type;
    private Double condition;
    private Double value;
    private Double valueLimit;
    private Integer status;

}
