package com.example.shoozy_shop.dto.response;

import lombok.*;
import org.checkerframework.checker.units.qual.N;

import java.util.List;
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ListCouponResponse {
    private List<CouponResponse> coupons;
    private int totalPage;
    private int totalElements;
}
