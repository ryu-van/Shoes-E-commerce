package com.example.shoozy_shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shoozy_shop.model.ReturnItemImage;

@Repository
public interface ReturnItemImageRepository extends JpaRepository<ReturnItemImage, Long> {
    List<ReturnItemImage> findByReturnItemId(Long returnItemId);

}
