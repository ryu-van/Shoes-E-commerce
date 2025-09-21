package com.example.shoozy_shop.repository;

import com.example.shoozy_shop.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByIsHidden(Boolean isHidden);

    // Cập nhật isHidden của review
    @Modifying
    @Query("UPDATE Review r SET r.isHidden = :isHidden WHERE r.id = :reviewId")
    void updateIsHidden(@Param("reviewId") Long reviewId, @Param("isHidden") Boolean isHidden);

    // Tìm review theo userId (nếu cần)
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId")
    List<Review> findByUserId(@Param("userId") Long userId);

    List<Review> findByProductId(Long productId);

    @Query("SELECT r FROM Review r WHERE r.user.id = :userId AND r.product.id = :productId")
    Review findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT r FROM Review r WHERE r.user.id = :userId AND r.product.id = :productId AND r.orderDetail.id = :orderDetailId")
    List<Review> findByUserIdAndProductIdAndOrderDetailId(@Param("userId") Long userId, @Param("productId") Long productId, @Param("orderDetailId") Long orderDetailId);

    List<Review> findByProductIdAndIsHiddenFalse(Long productId);
}
