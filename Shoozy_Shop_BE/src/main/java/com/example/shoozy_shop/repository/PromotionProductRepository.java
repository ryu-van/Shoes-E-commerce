package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.PromotionVariantDetailResponse;
import com.example.shoozy_shop.model.ProductPromotion;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionProductRepository extends JpaRepository<ProductPromotion, Long> {
    void deleteByPromotionId(Long id);

    @Query("""
                SELECT
                    pv.id AS idProductVariant,
                    p.id AS idProduct,
                    pv.size.value AS size,
                    pv.color.name AS color,
                    pp.customValue AS customValue,
                    pv.sellPrice AS originalPrice
                FROM ProductPromotion pp
                JOIN pp.productVariant pv
                JOIN pv.product p
                WHERE pp.promotion.id = :promotionId
            """)
    List<Tuple> findPromotionVariantTuplesByPromotionId(@Param("promotionId") Long promotionId);

    List<ProductPromotion> findByPromotionId(Long promotionId);

    ProductPromotion findByPromotionIdAndProductVariantId(Long promotionId, Long productId);

}
