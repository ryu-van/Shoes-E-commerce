package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.ProductVariantResponse;
import com.example.shoozy_shop.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariantResponse> findAllByProductId(Long productId);
    Optional<ProductVariant> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pv from ProductVariant pv where pv.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);
}
