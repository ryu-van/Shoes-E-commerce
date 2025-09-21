package com.example.shoozy_shop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantUpdateRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be >= 0")
    private Integer quantity;

    @NotNull(message = "Cost price is required")
    @Min(value = 0, message = "Cost price must be >= 0")
    private Double costPrice;

    @NotNull(message = "Sell price is required")
    @Min(value = 0, message = "Sell price must be >= 0")
    private Double sellPrice;
}
