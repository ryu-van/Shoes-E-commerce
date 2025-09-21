package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.*;
import com.example.shoozy_shop.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Boolean existsByNameIgnoreCase(String name);

    List<Product> findProductByCategoryIdAndIdNot(Long categoryId, Long idProduct);

    Boolean existsBySku(String sku);

    @Query("""
                SELECT new com.example.shoozy_shop.dto.response.TopSellingProductResponse(
                    p.id,
                    p.name,
                    p.thumbnail,
                    SUM(od.quantity)
                )
                FROM OrderDetail od
                JOIN ProductVariant pv ON od.productVariant.id = pv.id
                JOIN Product p ON pv.product.id = p.id
                JOIN Order o ON od.order.id = o.id
                WHERE o.status IN ('COMPLETED', 'DELIVERED')
                  AND o.createdAt >= :startDate
                  AND o.createdAt <= :endDate
                GROUP BY p.id, p.name, p.thumbnail
                ORDER BY SUM(od.quantity) DESC
            """)
    List<TopSellingProductResponse> findTopSellingProductsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
                SELECT new com.example.shoozy_shop.dto.response.LowStockProductResponse(
                    p.id,
                    p.name,
                    p.thumbnail,
                    SUM(pv.quantity)
                )
                FROM Product p
                JOIN ProductVariant pv ON pv.product.id = p.id
                WHERE p.status = true AND pv.status = true
                GROUP BY p.id, p.name, p.thumbnail
                HAVING SUM(pv.quantity) < 10
                ORDER BY SUM(pv.quantity) ASC
            """)
    List<LowStockProductResponse> findLowStockProducts();

    @Query("""
                SELECT p.id, p.name, sum(pv.quantity), pv.id, s.value, c.name, pv.sellPrice, pv.quantity
                                       FROM Product p
                                       JOIN ProductVariant pv ON pv.product.id = p.id
                                       LEFT JOIN pv.size s
                                       LEFT JOIN pv.color c
                                       WHERE p.status = true AND pv.status = true
                                       AND p.id IN :productIds
                                       GROUP BY p.id, p.name, pv.id, s.value, c.name, pv.sellPrice, pv.quantity
            """)
    List<Object[]> findAllActiveProductAndVariantsByProductIds(@Param("productIds") List<Long> productIds);

    @Query("""
                SELECT new com.example.shoozy_shop.dto.response.ProductPromotionResponse(
                    p.id,
                    p.name,
                    MIN(pv.sellPrice),
                    p.thumbnail,
                    SUM(pv.quantity)
                )
                FROM Product p
                JOIN ProductVariant pv ON pv.product.id = p.id
                WHERE p.status = true
                  AND pv.status = true
                  AND (
                    :productName IS NULL OR :productName = '' OR
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))
                  )
                  AND (
                    :categoryId IS NULL OR p.category.id = :categoryId
                  )
                GROUP BY p.id, p.name, p.thumbnail
            """)
    Page<ProductPromotionResponse> pageProductForPromotion(
            @Param("productName") String productName,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );


}
