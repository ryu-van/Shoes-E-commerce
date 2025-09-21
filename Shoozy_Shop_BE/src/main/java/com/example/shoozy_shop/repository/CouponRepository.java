package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.CouponResponse;
import com.example.shoozy_shop.model.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {


    @Query("""
              SELECT new com.example.shoozy_shop.dto.response.CouponResponse(
                  c.id, c.name, c.code, c.type, c.description, c.startDate, c.expirationDate,
                  c.value, c.condition, c.status, c.quantity, c.valueLimit
              )
              FROM Coupon c
              WHERE (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
                                 OR LOWER(c.code) LIKE LOWER(CONCAT('%', :name, '%')))
                AND (:startDate IS NULL OR cast(c.expirationDate as date) >= :startDate)
                AND (:expirationDate IS NULL OR cast(c.startDate as date) <= :expirationDate)
                AND (:status IS NULL OR c.status = :status)
              ORDER BY c.id DESC
            """)
    Page<CouponResponse> getCouponPage(
            @Param("name") String name,
            @Param("startDate") LocalDate startDate,
            @Param("expirationDate") LocalDate expirationDate,
            @Param("status") Integer status,
            Pageable pageable
    );


    Coupon findByCode(String code);

    @Query("""
                SELECT c FROM Coupon c
                WHERE
                    c.value < :orderValue
                    AND c.status = 1
                    AND c.expirationDate >= CURRENT_DATE
                    AND c.id NOT IN (
                        SELECT o.coupon.id FROM Order o WHERE o.user.id = :userId AND o.coupon IS NOT NULL
                    )
            """)
    List<Coupon> findAvailableCouponsForUser(
            @Param("orderValue") double orderValue,
            @Param("userId") long userId
    );

    @Query("""
                SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
                FROM Order o
                WHERE o.coupon.code = :codeCoupon
                  AND o.user.id = :userId
                  AND (:userId <> 1)
            """)
    Boolean checkCouponInUse(@Param("codeCoupon") String codeCoupon,
                             @Param("userId") Long userId);


    @Modifying
    @Query("UPDATE Coupon c SET c.quantity = c.quantity - 1 WHERE c.id = :id AND c.quantity > 0")
    int decrementIfAvailable(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Coupon c SET c.status = :status WHERE c.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Query("select c.quantity from Coupon c where c.id = :id")
    Integer getRemaining(@Param("id") Long id);

    List<Coupon> findAllByStatus(Integer status);

    @Query("SELECT c FROM Coupon c WHERE " +
            "c.quantity = 0 OR " +
            "c.startDate <= CURRENT_TIMESTAMP OR " +
            "c.expirationDate <= CURRENT_TIMESTAMP")
    List<Coupon> findCouponsNeedingStatusUpdate();


}
