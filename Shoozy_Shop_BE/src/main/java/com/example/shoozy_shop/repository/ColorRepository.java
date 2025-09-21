package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColorRepository extends JpaRepository<Color, Long> {
    List<Color> findAllByStatusTrue();
    Boolean existsByName(String name);
}
