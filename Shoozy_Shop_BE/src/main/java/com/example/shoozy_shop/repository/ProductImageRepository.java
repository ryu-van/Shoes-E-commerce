package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.ProductImageByColorResponse;
import com.example.shoozy_shop.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    @Query("""
                SELECT DISTINCT new com.example.shoozy_shop.dto.response.ProductImageByColorResponse(
                    pi.id,
                    pi.imageUrl
                )
                FROM ProductImage pi
                JOIN ProductVariantImage pvi ON pi.id = pvi.productImage.id
                JOIN ProductVariant pv ON pv.id = pvi.productVariant.id
                JOIN Product p ON pv.product.id = p.id
                JOIN Color c ON c.id = pv.color.id
                WHERE c.id = :colorId
                ORDER BY pi.id
            """)
    List<ProductImageByColorResponse> getImagesByColor(@Param("colorId") Long colorId);

}
