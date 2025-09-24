package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN FETCH t.vnPayTransactionDetail " +
            "WHERE t.order.id = :orderId")
    List<Transaction> findByOrderId(@Param("orderId") Long orderId);

    // Lấy transaction theo userId
    List<Transaction> findByOrder_User_Id(Long userId);

    // Xóa tất cả transaction theo userId
    void deleteByOrder_User_Id(Long userId);

    // Nếu cần lấy toàn bộ transactionId cho 1 user
    List<Transaction> findAllByOrder_User_Id(Long userId);

    Transaction findByTransactionCode(String transactionCode);

    // Trả về giao dịch PENDING mới nhất theo order.orderCode
    Optional<Transaction> findTopByOrder_OrderCodeAndStatusOrderByCreatedAtDesc(String orderCode, String status);

    Transaction findByOrder_OrderCode(String orderCode);



}
