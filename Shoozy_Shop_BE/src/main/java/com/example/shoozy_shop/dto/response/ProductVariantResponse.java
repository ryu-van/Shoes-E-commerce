package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.model.Color;
import com.example.shoozy_shop.model.ProductVariant;
import com.example.shoozy_shop.model.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProductVariantResponse {
    private Long id;

    @JsonProperty("product_id")
    private Long productId;

    private Size size;
    private Color color;

    private Integer quantity;

    private Double costPrice;

    private Double sellPrice;

    private String thumbnail;
    private List<ProductImageResponse> images;

    // Thêm promotion với custom value cho variant
    @JsonProperty("promotions")
    private List<VariantPromotionResponse> promotions;

    public static ProductVariantResponse fromProductVariant(ProductVariant productVariant) {
        // Lấy promotion với custom value cho variant
        List<VariantPromotionResponse> variantPromotions = getActivePromotionsForVariant(productVariant);

        return ProductVariantResponse.builder()
                .id(productVariant.getId())
                .productId(productVariant.getProduct().getId())
                .size(productVariant.getSize())
                .color(productVariant.getColor())
                .quantity(productVariant.getQuantity())
                .costPrice(productVariant.getCostPrice())   // lấy từ entity
                .sellPrice(productVariant.getSellPrice())   // lấy từ entity
                .thumbnail(productVariant.getThumbnail())
                .promotions(variantPromotions)
                .images(
                        productVariant.getProductVariantImages().stream()
                                .map(variantImage -> ProductImageResponse.fromEntity(variantImage.getProductImage()))
                                .toList()
                )
                .build();
    }

    // Helper method để lấy promotion với custom value cho variant
    private static List<VariantPromotionResponse> getActivePromotionsForVariant(ProductVariant variant) {
        LocalDateTime now = LocalDateTime.now();

        return variant.getProductPromotions().stream()
                .filter(pp -> pp.getPromotion().getStatus() == 1) // Active status
                .filter(pp -> !pp.getPromotion().getStartDate().isAfter(now)) // startDate <= now
                .filter(pp -> !pp.getPromotion().getExpirationDate().isBefore(now)) // expirationDate >= now
                .map(pp -> VariantPromotionResponse.builder()
                        .id(pp.getPromotion().getId())
                        .name(pp.getPromotion().getName())
                        .code(pp.getPromotion().getCode())
                        .startDate(pp.getPromotion().getStartDate())
                        .expirationDate(pp.getPromotion().getExpirationDate())
                        .originalValue(pp.getPromotion().getValue()) // Value gốc từ promotion
                        .customValue(pp.getCustomValue()) // Custom value cho variant cụ thể
                        .description(pp.getPromotion().getDescription())
                        .status(pp.getPromotion().getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}
