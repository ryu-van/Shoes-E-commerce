package com.example.shoozy_shop.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.poi.hpsf.Decimal;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class FeeShippingResponse {
    private BigDecimal total;
    private String currency;
    private Integer serviceTypeIdUsed;
    private Integer toDistrictId;
    private String toWardCode;

}
