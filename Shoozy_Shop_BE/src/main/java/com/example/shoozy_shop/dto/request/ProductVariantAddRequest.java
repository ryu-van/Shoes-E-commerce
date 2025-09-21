package com.example.shoozy_shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantAddRequest {

    @NotNull(message = "Product ID is required")
    @JsonProperty("product_id")
    private Long productId;

    @NotNull(message = "Size ID is required")
    @JsonProperty("size_id")
    private Long sizeId;

    @NotNull(message = "Color ID is required")
    @JsonProperty("color_id")
    private Long colorId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be >= 0")
    @JsonProperty("quantity")
    private Integer quantity;

    @NotNull(message = "Cost price is required")
    @Min(value = 0, message = "Cost price must be >= 0")
    @JsonProperty("cost_price")
    private Double costPrice;

    @NotNull(message = "Sell price is required")
    @Min(value = 0, message = "Sell price must be >= 0")
    @JsonProperty("sell_price")
    private Double sellPrice;
}
