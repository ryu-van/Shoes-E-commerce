package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.OrderTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, Long> {
    List<OrderTimeline> findByOrderIdOrderByCreateDateDesc(Long orderId);
}