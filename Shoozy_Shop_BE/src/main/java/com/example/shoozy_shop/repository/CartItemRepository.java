package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.ProductCartResponse;
import com.example.shoozy_shop.dto.response.ProductCheckoutResponse;
import com.example.shoozy_shop.model.Cart;
import com.example.shoozy_shop.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    CartItem findByCartIdAndProductVariantId(Long cartId, Long productVariantId);

    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    // Lấy toàn bộ sản phẩm trong giỏ hàng
    @Query("""
                SELECT new com.example.shoozy_shop.dto.response.ProductCartResponse(
                    ci.id,
                    p.id,
                    p.name,
                    pv.thumbnail,
                    pv.sellPrice,
                    CAST(COALESCE(MAX(COALESCE(pp.customValue, pr.value)), 0) AS double),
                    s.value,
                    c.name,
                    ci.quantity,
                    pv.quantity
                )
                FROM CartItem ci
                JOIN ci.productVariant pv
                JOIN pv.product p
                LEFT JOIN pv.size s
                LEFT JOIN pv.color c
                LEFT JOIN pv.productPromotions pp
                LEFT JOIN pp.promotion pr
                WHERE ci.cart.id = :cartId
                  AND (
                        pr IS NULL
                        OR (
                            pr.status = 1
                            AND CURRENT_TIMESTAMP BETWEEN pr.startDate AND pr.expirationDate
                        )
                      )
                GROUP BY
                    ci.id, p.name, pv.thumbnail, pv.sellPrice,
                    s.value, c.name, ci.quantity, pv.quantity,p.id
            """)
    List<ProductCartResponse> findCartItemsByCartId(@Param("cartId") Long cartId);

    @Query(value = """
            SELECT 
                ci.id, pv.id, p.name, p.thumbnail, pv.sell_price,
                s.value, c.name, ci.quantity, p.weight,
                (SELECT TOP 1 pr2.id 
                 FROM product_promotions pp2 
                 JOIN promotions pr2 ON pp2.promotion_id = pr2.id
                 WHERE pp2.product_variant_id = pv.id 
                   AND pr2.status = 1 
                   AND SYSDATETIME() BETWEEN pr2.start_date AND pr2.expiration_date
                 ORDER BY ISNULL(pp2.custom_value, pr2.value) DESC, pr2.id ASC),
                ISNULL((SELECT MAX(ISNULL(pp3.custom_value, pr3.value))
                        FROM product_promotions pp3 
                        JOIN promotions pr3 ON pp3.promotion_id = pr3.id
                        WHERE pp3.product_variant_id = pv.id 
                          AND pr3.status = 1 
                          AND SYSDATETIME() BETWEEN pr3.start_date AND pr3.expiration_date), 0.0)
            FROM cart_items ci
            JOIN product_variants pv ON ci.product_variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN sizes s ON pv.size_id = s.id
            LEFT JOIN colors c ON pv.color_id = c.id
            WHERE ci.id IN :cartItemIds
            """, nativeQuery = true)
    List<Object[]> findProductCheckoutRaw(@Param("cartItemIds") List<Long> cartItemIds);


    // Wrapper để map sang DTO
    default List<ProductCheckoutResponse> findProductCheckoutByCartItemId(List<Long> cartItemIds) {
        List<Object[]> rawResults = findProductCheckoutRaw(cartItemIds);

        return rawResults.stream()
                .map(row -> new ProductCheckoutResponse(
                        ((Number) row[0]).longValue(),   // ci.id
                        ((Number) row[1]).longValue(),   // pv.id
                        (String) row[2],                 // p.name
                        (String) row[3],                 // p.thumbnail
                        row[4] != null ? ((Number) row[4]).doubleValue() : null, // pv.price
                        row[5] != null ? row[5].toString() : null,               // s.value -> String
                        (String) row[6],                 // c.name
                        ((Number) row[7]).intValue(),    // ci.quantity
                        row[8] != null ? ((Number) row[8]).doubleValue() : null, // p.weight
                        row[9] != null ? ((Number) row[9]).longValue() : null,   // promotion_id
                        row[10] != null ? ((Number) row[10]).doubleValue() : 0.0 // promotion_value
                ))
                .collect(Collectors.toList());
    }


}

