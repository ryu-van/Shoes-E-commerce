package com.example.shoozy_shop.dto.request;

import lombok.Data;

@Data
public class OpenApiRequest {
    private Integer provinceCode;
    private Integer districtCode;
    private Integer wardCode;

    private String provinceName;
    private String districtName;
    private String wardName;

    private Integer weight;
    
    private Integer serviceTypeId;
    
    private Integer insuranceValue;
}
