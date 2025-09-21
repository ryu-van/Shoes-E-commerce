package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrderId(Long orderId);

    @Query("SELECT COALESCE(SUM(od.quantity * pv.costPrice), 0) " +
            "FROM OrderDetail od " +
            "JOIN od.order o " +
            "JOIN od.productVariant pv " +
            "WHERE YEAR(o.createdAt) = :year " +
            "AND MONTH(o.createdAt) = :month " +
            "AND o.status = 'COMPLETED' " +
            "AND (:type IS NULL OR o.type = :type)")
    BigDecimal sumCostByMonthAndType(@Param("year") int year,
                                     @Param("month") int month,
                                     @Param("type") Boolean type);

}
