package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.model.*;
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
public class ProductResponse extends BaseResponse {
    private Long id;
    private String sku;
    private String name;
    private Brand brand;
    private Category category;
    private String gender;
    private Double weight;
    private Material material;
    private String thumbnail;
    private String description;
    private Boolean status;
    private Integer totalQuantity;
    private Double minPrice;
    private Double maxPrice;
    private Double averageRating;

    // Thêm promotion cho màn hình chung
    @JsonProperty("active_promotions")
    private List<PromotionResponse> activePromotions;

    @JsonProperty("variants")
    private List<ProductVariantResponse> variants;

    @JsonProperty("reviews")
    private List<ReviewResponseDTO> reviews;

    // getAll products - Hiển thị promotion chung
    public static ProductResponse fromProduct(Product product, List<Long> sizeIds, List<Long> colorIds, List<Long> materialIds) {
        int totalQuantity = product.getProductVariants()
                .stream()
                .mapToInt(ProductVariant::getQuantity)
                .sum();

        Double minPrice = product.getProductVariants()
                .stream()
                .mapToDouble(ProductVariant::getSellPrice) // dùng sellPrice
                .min()
                .orElse(0.0);

        Double maxPrice = product.getProductVariants()
                .stream()
                .mapToDouble(ProductVariant::getSellPrice) // dùng sellPrice
                .max()
                .orElse(0.0);

        Double avgRating = product.getReviews().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsHidden()))
                .filter(r -> r.getRating() != null)
                .mapToInt(r -> r.getRating())
                .average()
                .orElse(0.0);

        List<Review> allReviews = product.getReviews().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsHidden()))
                .collect(Collectors.toList());
        List<ReviewResponseDTO> parentReviews = allReviews.stream()
                .filter(r -> r.getParent() == null)
                .map(r -> mapReviewWithReplies(r, allReviews))
                .collect(Collectors.toList());

        // Lấy promotion active cho sản phẩm (cho màn hình chung)
        List<PromotionResponse> activePromotions = getActivePromotionsForProduct(product);

        ProductResponse productResponse = ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .thumbnail(product.getThumbnail())
                .description(product.getDescription())
                .brand(product.getBrand())
                .category(product.getCategory())
                .gender(product.getGender())
                .weight(product.getWeight())
                .material(product.getMaterial())
                .status(product.getStatus())
                .totalQuantity(totalQuantity)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .averageRating(avgRating)
                .activePromotions(activePromotions) // Thêm promotion
                .variants(
                        product.getProductVariants()
                                .stream()
                                .filter(variant -> sizeIds == null || sizeIds.isEmpty() || sizeIds.contains(variant.getSize().getId()))
                                .filter(variant -> colorIds == null || colorIds.isEmpty() || colorIds.contains(variant.getColor().getId()))
                                .map(ProductVariantResponse::fromProductVariant)
                                .collect(Collectors.toList())
                )
                .reviews(parentReviews)
                .build();

        productResponse.setCreatedAt(product.getCreatedAt());
        productResponse.setUpdatedAt(product.getUpdatedAt());
        return productResponse;
    }

    // product-detail - Hiển thị promotion chi tiết
    public static ProductResponse fromProduct(Product product) {
        int totalQuantity = product.getProductVariants()
                .stream()
                .mapToInt(ProductVariant::getQuantity)
                .sum();

        Double minPrice = product.getProductVariants()
                .stream()
                .mapToDouble(ProductVariant::getSellPrice) // dùng sellPrice
                .min()
                .orElse(0.0);

        Double maxPrice = product.getProductVariants()
                .stream()
                .mapToDouble(ProductVariant::getSellPrice) // dùng sellPrice
                .max()
                .orElse(0.0);

        Double avgRating = product.getReviews().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsHidden()))
                .filter(r -> r.getRating() != null)
                .mapToInt(r -> r.getRating())
                .average()
                .orElse(0.0);

        List<Review> allReviews = product.getReviews().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsHidden()))
                .collect(Collectors.toList());
        List<ReviewResponseDTO> parentReviews = allReviews.stream()
                .filter(r -> r.getParent() == null)
                .map(r -> mapReviewWithReplies(r, allReviews))
                .collect(Collectors.toList());

        // Lấy promotion active cho sản phẩm
        List<PromotionResponse> activePromotions = getActivePromotionsForProduct(product);

        ProductResponse productResponse = ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .thumbnail(product.getThumbnail())
                .description(product.getDescription())
                .brand(product.getBrand())
                .category(product.getCategory())
                .gender(product.getGender())
                .weight(product.getWeight())
                .material(product.getMaterial())
                .status(product.getStatus())
                .totalQuantity(totalQuantity)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .averageRating(avgRating)
                .activePromotions(activePromotions) // Thêm promotion
                .variants(
                        product.getProductVariants()
                                .stream()
                                .map(ProductVariantResponse::fromProductVariant)
                                .collect(Collectors.toList())
                )
                .reviews(parentReviews)
                .build();

        productResponse.setCreatedAt(product.getCreatedAt());
        productResponse.setUpdatedAt(product.getUpdatedAt());
        return productResponse;
    }

    // Helper method để lấy promotion active cho sản phẩm
    private static List<PromotionResponse> getActivePromotionsForProduct(Product product) {
        LocalDateTime now = LocalDateTime.now();

        return product.getProductVariants().stream()
                .flatMap(variant -> variant.getProductPromotions().stream())
                .map(ProductPromotion::getPromotion)
                .filter(promotion -> promotion.getStatus() == 1) // Active status
                .filter(promotion -> promotion.getStartDate().isBefore(now) || promotion.getStartDate().isEqual(now))
                .filter(promotion -> promotion.getExpirationDate().isAfter(now) || promotion.getExpirationDate().isEqual(now))
                .distinct()
                .map(PromotionResponse::fromPromotion)
                .collect(Collectors.toList());
    }

    private static ReviewResponseDTO mapReviewWithReplies(Review review, List<Review> allReviews) {
        List<ReviewResponseDTO> replies = allReviews.stream()
                .filter(r -> r.getParent() != null && r.getParent().getId().equals(review.getId()))
                .map(r -> mapReviewWithReplies(r, allReviews))
                .collect(Collectors.toList());
        return new ReviewResponseDTO(
                review.getId(),
                review.getContent(),
                review.getRating(),
                review.getUser() != null ? review.getUser().getId() : null,
                review.getUser() != null ? review.getUser().getFullname() : null,
                review.getProduct() != null ? review.getProduct().getId() : null,
                review.getCreatedAt() != null ? review.getCreatedAt().toString() : null,
                review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null,
                review.getOrderDetail() != null ? review.getOrderDetail().getId() : null,
                replies
        );
    }
}
