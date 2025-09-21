package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.model.Promotion;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

@SuperBuilder

public class PromotionResponse {

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

    public static PromotionResponse fromPromotion(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .code(promotion.getCode())
                .startDate(promotion.getStartDate())
                .expirationDate(promotion.getExpirationDate())
                .value(promotion.getValue())
                .description(promotion.getDescription())
                .status(promotion.getStatus())
                .build();
    }

}
