package com.example.shoozy_shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shoozy_shop.model.VnPayTransactionDetail;

@Repository
public interface VnPayTransactionDetailRepository extends JpaRepository<VnPayTransactionDetail, Long> {
    // Xóa theo transactionId
    void deleteByTransaction_Id(Long transactionId);

    // Hoặc xóa tất cả theo nhiều transactionId
    void deleteAllByTransaction_IdIn(Iterable<Long> transactionIds);

}
