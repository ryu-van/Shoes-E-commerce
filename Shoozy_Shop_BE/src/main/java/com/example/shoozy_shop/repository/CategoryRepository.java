package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByStatusTrue();
    Boolean existsByName(String name);
}
