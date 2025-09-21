package com.example.shoozy_shop.dto.response;
import com.example.shoozy_shop.model.ProductImage;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class ProductImageResponse {
    private Long id;
    private String url;

    public static ProductImageResponse fromEntity(ProductImage productImage) {
        return ProductImageResponse.builder()
                .id(productImage.getId())
                .url(productImage.getImageUrl())
                .build();
    }
}
