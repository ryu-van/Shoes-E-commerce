package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.DailySummary;
import com.example.shoozy_shop.dto.response.OrderStatusStatisticsDto;
import com.example.shoozy_shop.dto.response.OrderSummary;
import com.example.shoozy_shop.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Query chính không lấy transactions
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.paymentMethod " +
            "LEFT JOIN FETCH o.coupon " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.productVariant pv " +
            "LEFT JOIN FETCH pv.product p " +
            "LEFT JOIN FETCH p.category " +
            "WHERE o.id = :orderId")
    Order getOrderWithDetails(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.paymentMethod " +
            "LEFT JOIN FETCH o.coupon " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.productVariant pv " +
            "LEFT JOIN FETCH pv.product p " +
            "LEFT JOIN FETCH p.category " +
            "WHERE o.user.id = :userId " +
            "ORDER BY o.id DESC")
    List<Order> getAllOrderByUserId(@Param("userId") Long userId);

    void deleteByUser_Id(Long userId);

    @Query("""
    SELECT new com.example.shoozy_shop.dto.response.OrderStatusStatisticsDto(
        o.status,
        COUNT(o.id)
    )
    FROM Order o
    WHERE o.createdAt BETWEEN :startDate AND :endDate
    GROUP BY o.status
    ORDER BY COUNT(o.id) DESC
    """)
    List<OrderStatusStatisticsDto> getOrderStatusStatisticsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    Boolean existsByOrderCode(String orderCode);

    Order findByOrderCode(String orderCode);

    // Thống kê đơn hàng tại quầy (type = 0 = offline)
    @Query("SELECT new com.example.shoozy_shop.dto.response.DailySummary(" +
            "COUNT(o), COALESCE(SUM(o.finalPrice), 0)) " +
            "FROM Order o " +
            "WHERE CAST(o.createdAt AS date) = :specificDate " +
            "AND o.status = 'COMPLETED' " +
            "AND o.type = false")
    DailySummary countOfflineOrdersByDate(@Param("specificDate") LocalDate specificDate);

    // Thống kê đơn hàng online (type = 1 = online)
    @Query("SELECT new com.example.shoozy_shop.dto.response.DailySummary(" +
            "COUNT(o), COALESCE(SUM(o.finalPrice), 0)) " +
            "FROM Order o " +
            "WHERE CAST(o.createdAt AS date) = :specificDate " +
            "AND o.status = 'COMPLETED' " +
            "AND o.type = true")
    DailySummary countOnlineOrdersByDate(@Param("specificDate") LocalDate specificDate);

    // Thống kê tổng số đơn hàng trong ngày
    @Query("SELECT new com.example.shoozy_shop.dto.response.DailySummary(" +
            "COUNT(o), COALESCE(SUM(o.finalPrice), 0)) " +
            "FROM Order o " +
            "WHERE CAST(o.createdAt AS date) = :specificDate " +
            "AND o.status = 'COMPLETED'")
    DailySummary countTotalOrdersByDate(@Param("specificDate") LocalDate specificDate);

    // Thống kê lợi nhuận, doanh thu,...
    @Query("SELECT COALESCE(SUM(o.finalPrice - o.shippingFee), 0) " +
            "FROM Order o " +
            "WHERE YEAR(o.createdAt) = :year " +
            "AND MONTH(o.createdAt) = :month " +
            "AND o.status = 'COMPLETED' " +
            "AND (:type IS NULL OR o.type = :type)")
    BigDecimal sumRevenueByMonthAndType(@Param("year") int year,
                                        @Param("month") int month,
                                        @Param("type") Boolean type);


}