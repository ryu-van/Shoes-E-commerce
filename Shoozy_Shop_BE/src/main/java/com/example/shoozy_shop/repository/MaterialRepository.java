package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> getAllByStatusTrue();
    Boolean existsByName(String name);
}
