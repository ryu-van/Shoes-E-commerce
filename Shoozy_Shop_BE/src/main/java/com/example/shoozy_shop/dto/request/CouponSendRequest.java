package com.example.shoozy_shop.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CouponSendRequest {

    private List<String> to;

    private String subject;

    private String codeCoupon;

    private Boolean typeCoupon;

    private Double valueCoupon;

    private Double valueLimit;

    private String  startDate;

    private String endDate;

    private String condition;

    private String homeUrl;

}
