// com/example/shoozy_shop/repository/RefundInfoRepository.java
package com.example.shoozy_shop.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.shoozy_shop.model.RefundInfo;

public interface RefundInfoRepository extends JpaRepository<RefundInfo, Long> {
    Optional<RefundInfo> findByReturnRequestId(Long returnRequestId);
}
