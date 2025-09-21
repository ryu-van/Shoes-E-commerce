package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.ProductVariantImageResponse;
import com.example.shoozy_shop.model.ProductVariantImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, Long> {
    @Query("SELECT new com.example.shoozy_shop.dto.response.ProductVariantImageResponse(" +
            "pvi.productVariant.id, pvi.productImage.imageUrl) " +
            "FROM ProductVariantImage pvi " +
            "WHERE pvi.productVariant.id IN :variantIds")
    List<ProductVariantImageResponse> findImagesByVariantIds(@Param("variantIds") List<Long> variantIds);
    Optional<ProductVariantImage> findByProductVariantIdAndProductImageId(Long variantId, Long imageId);
}
