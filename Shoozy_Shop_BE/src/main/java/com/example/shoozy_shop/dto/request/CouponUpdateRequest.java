package com.example.shoozy_shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CouponUpdateRequest {
    private String name;

    private String description;

    @JsonProperty("type")
    private Boolean type;

    @JsonProperty("start_date")
    private LocalDateTime startDate;

    @JsonProperty("expiration_date")
    private LocalDateTime expirationDate;

    private Double value;

    private Double condition;

    private Integer quantity;

    @JsonProperty("value_limit")
    private Double valueLimit;
}
