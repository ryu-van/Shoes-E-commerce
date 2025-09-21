package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChartResponse {
    private String period; // "Jan", "Feb", "1", "2", "2024", etc.
    private BigDecimal revenue; // Doanh thu (màu vàng)
    private BigDecimal cost; // Giá vốn (màu xanh nhạt)
    private BigDecimal profit; // Lợi nhuận (đường đỏ)
}