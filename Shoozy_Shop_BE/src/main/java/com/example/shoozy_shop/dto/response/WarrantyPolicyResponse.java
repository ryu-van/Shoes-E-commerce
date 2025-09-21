package com.example.shoozy_shop.dto.response;

import lombok.Data;

@Data
public class WarrantyPolicyResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private Integer warrantyMonths;
    private Boolean active;
}