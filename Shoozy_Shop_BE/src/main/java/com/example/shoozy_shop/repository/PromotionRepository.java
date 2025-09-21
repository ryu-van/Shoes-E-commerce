package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.PromotionResponse;
import com.example.shoozy_shop.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("""
                SELECT new com.example.shoozy_shop.dto.response.PromotionResponse(
                    p.id, p.name, p.code, p.startDate, p.expirationDate,
                    p.status, p.value, p.description
                )
                FROM Promotion p
                WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                       OR  LOWER(p.code)  LIKE LOWER(CONCAT('%', :keyword, '%')))
                  AND (:startDate IS NULL OR cast(p.expirationDate as date) >= :startDate)
                  AND (:endDate   IS NULL OR cast(p.startDate     as date) <= :endDate)
                  AND (:status IS NULL OR p.status = :status)
                ORDER BY p.id DESC
            """)
    Page<PromotionResponse> findPromotionInfoWithProductCount(
            @Param("keyword") String keyword,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") Integer status,
            Pageable pageable
    );


    @Query("""
                SELECT new com.example.shoozy_shop.dto.response.PromotionResponse(
                    p.id,
                    p.name,
                    p.code,
                    p.startDate,
                    p.expirationDate,
                    p.status,
                    p.value,
                    p.description
                )
                FROM ProductPromotion pp
                RIGHT JOIN pp.promotion p
                WHERE p.id = :id
                GROUP BY p.id, p.name, p.code, p.startDate, p.expirationDate, p.status, p.value,p.description
            """)
    PromotionResponse findPromotionInfoById(@Param("id") Long id);


}
