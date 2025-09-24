package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
    Optional<RefundTransaction> findByReturnRequestId(Long returnRequestId);

    boolean existsByReturnRequestId(Long returnRequestId);
}
