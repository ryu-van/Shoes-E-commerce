package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailySummary {
    private Long count;
    private BigDecimal revenue;

    // Constructor chỉ với count (cho trường hợp chỉ đếm số đơn)
    public DailySummary(Long count) {
        this.count = count;
        this.revenue = BigDecimal.ZERO;
    }
}