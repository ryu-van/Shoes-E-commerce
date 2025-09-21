package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.dto.response.ProductVariantResponse;
import com.example.shoozy_shop.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariantResponse> findAllByProductId(Long productId);
    Optional<ProductVariant> findById(Long id);
}
