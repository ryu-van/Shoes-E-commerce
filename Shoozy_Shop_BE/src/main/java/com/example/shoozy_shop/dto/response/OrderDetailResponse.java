package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.enums.RefundStatus;
import com.example.shoozy_shop.model.OrderDetail;
import com.example.shoozy_shop.model.ProductVariant;
import com.example.shoozy_shop.model.Review;
import com.example.shoozy_shop.repository.ReviewRepository;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class OrderDetailResponse {
        private Long orderDetailId;
        private Long productId;
        private Long productVariantId;
        private String productName;
        private String thumbnail;
        private String color;
        private Integer size;
        private Double price;
        private Integer quantity;
        private Double totalMoney;
        private Boolean status;
        private RefundStatus refundStatus;
        private Integer refundedQuantity; // số lượng đã hoàn tiền

        // Thông tin review
        private Long reviewId;
        private String reviewContent;
        private Integer reviewRating;

        // Hàm cũ giữ nguyên
        public static OrderDetailResponse fromEntity(OrderDetail orderDetail) {
                ProductVariant pv = orderDetail.getProductVariant();
                return OrderDetailResponse.builder()
                                .orderDetailId(orderDetail.getId())
                                .productId(pv.getProduct().getId())
                                .productVariantId(pv.getId())
                                .productName(pv.getProduct().getName())
                                .thumbnail(pv.getThumbnail())
                                .color(pv.getColor().getName())
                                .size(pv.getSize().getValue())
                                .price(orderDetail.getPrice())
                                .quantity(orderDetail.getQuantity())
                                .totalMoney(orderDetail.getTotalMoney())
                                .status(orderDetail.getStatus())
                                .refundStatus(orderDetail.getRefundStatus())
                                .build();
        }

        // Hàm mới lấy thêm review
        public static OrderDetailResponse fromEntity(OrderDetail orderDetail, Long userId,
                        ReviewRepository reviewRepository) {
                ProductVariant pv = orderDetail.getProductVariant();
                Review review = reviewRepository
                                .findByUserIdAndProductIdAndOrderDetailId(userId, pv.getProduct().getId(),
                                                orderDetail.getId())
                                .stream().findFirst().orElse(null);
                return OrderDetailResponse.builder()
                                .orderDetailId(orderDetail.getId())
                                .productId(pv.getProduct().getId())
                                .productVariantId(pv.getId())
                                .productName(pv.getProduct().getName())
                                .thumbnail(pv.getThumbnail())
                                .color(pv.getColor().getName())
                                .size(pv.getSize().getValue())
                                .price(orderDetail.getPrice())
                                .quantity(orderDetail.getQuantity())
                                .totalMoney(orderDetail.getTotalMoney())
                                .status(orderDetail.getStatus())
                                .refundStatus(orderDetail.getRefundStatus())
                                .reviewId(review != null ? review.getId() : null)
                                .reviewContent(review != null ? review.getContent() : null)
                                .reviewRating(review != null ? review.getRating() : null)
                                .build();
        }

        public static OrderDetailResponse fromEntity(OrderDetail orderDetail, Long userId,
                        ReviewRepository reviewRepository, int refundedQty) {
                ProductVariant pv = orderDetail.getProductVariant();
                Review review = reviewRepository
                                .findByUserIdAndProductIdAndOrderDetailId(userId, pv.getProduct().getId(),
                                                orderDetail.getId())
                                .stream().findFirst().orElse(null);

                return OrderDetailResponse.builder()
                                .orderDetailId(orderDetail.getId())
                                .productId(pv.getProduct().getId())
                                .productVariantId(pv.getId())
                                .productName(pv.getProduct().getName())
                                .thumbnail(pv.getThumbnail())
                                .color(pv.getColor().getName())
                                .size(pv.getSize().getValue())
                                .price(orderDetail.getPrice())
                                .quantity(orderDetail.getQuantity())
                                .totalMoney(orderDetail.getTotalMoney())
                                .status(orderDetail.getStatus())
                                .refundStatus(orderDetail.getRefundStatus())
                                .refundedQuantity(refundedQty) // <-- thêm đây
                                .reviewId(review != null ? review.getId() : null)
                                .reviewContent(review != null ? review.getContent() : null)
                                .reviewRating(review != null ? review.getRating() : null)
                                .build();
        }

}
