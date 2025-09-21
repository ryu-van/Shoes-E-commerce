package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findAllByStatusTrue();
    Boolean existsByName(String name);
}
