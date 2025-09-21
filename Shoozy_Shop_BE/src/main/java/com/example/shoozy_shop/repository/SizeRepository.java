package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SizeRepository extends JpaRepository<Size, Long> {
    List<Size> findAllByStatusTrue();
    Boolean existsByValue(Integer value);
}
