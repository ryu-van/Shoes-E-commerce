package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.paymentMethod " +
            "LEFT JOIN FETCH o.coupon " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.productVariant pv " +
            "LEFT JOIN FETCH pv.product " +
            "LEFT JOIN FETCH pv.size " +
            "LEFT JOIN FETCH pv.color " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(Long orderId);

    // Thêm phương thức để lấy hóa đơn theo danh sách trạng thái
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.paymentMethod " +
            "LEFT JOIN FETCH o.coupon " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.productVariant pv " +
            "LEFT JOIN FETCH pv.product " +
            "LEFT JOIN FETCH pv.size " +
            "LEFT JOIN FETCH pv.color " +
            "WHERE o.status IN :statuses")
    List<Order> findAllByStatusIn(List<String> statuses);
}