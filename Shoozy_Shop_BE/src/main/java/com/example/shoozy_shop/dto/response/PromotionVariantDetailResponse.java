package com.example.shoozy_shop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PromotionVariantDetailResponse  {


    private Long idProductVariant;

    private Long idProduct;

    private Integer size;

    private String color;


    @JsonProperty("custom_value")
    private Double customValue;

    private Double originalPrice;


}

