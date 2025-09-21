package com.example.shoozy_shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class PromotionDetailResponse  {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("code")
    private String code;

    @JsonProperty("start_date")
    private LocalDateTime startDate;

    @JsonProperty("expiration_date")
    private LocalDateTime expirationDate;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("value")
    private Double value;

    @JsonProperty("description")
    private String description;

    private List<PromotionVariantDetailResponse> promotionVariantDetailResponses;
}
