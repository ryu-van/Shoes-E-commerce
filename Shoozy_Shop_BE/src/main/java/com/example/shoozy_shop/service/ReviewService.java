package com.example.shoozy_shop.service;

import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Review;
import com.example.shoozy_shop.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shoozy_shop.dto.request.ReviewRequest;
import com.example.shoozy_shop.model.Product;
import com.example.shoozy_shop.model.User;
import com.example.shoozy_shop.repository.ProductRepository;
import com.example.shoozy_shop.repository.UserRepository;
import com.example.shoozy_shop.dto.response.ReviewResponseDTO;
import com.example.shoozy_shop.model.OrderDetail;
import com.example.shoozy_shop.repository.OrderDetailRepository;
import com.example.shoozy_shop.service.WebSocketService;
import com.example.shoozy_shop.dto.response.ReviewInfoDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final WebSocketService webSocketService;

    // Lấy tất cả review
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    // Lấy review theo ID
    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("review", id));
    }

    // Cập nhật review
    @Transactional
    public Review updateReview(Long id, Review review) {
        Review existingReview = getReviewById(id);
        existingReview.setContent(review.getContent());
        existingReview.setIsHidden(review.getIsHidden());
        existingReview.setRating(review.getRating());
        return reviewRepository.save(existingReview);
    }

//    public List<Review> getReviewsByProductId(Long productId) {
//        return reviewRepository.findByProductId(productId);
//    }
    // Thêm bình luận (giả định Comment là thực thể riêng)
//    @Transactional
//    public Comment addComment(Long reviewId, Comment comment) {
//        Review review = getReviewById(reviewId);
//        comment.setReview(review); // Liên kết với review
//        return commentRepository.save(comment); // Cần inject CommentRepository
//    }

    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review không tồn tại"));
        reviewRepository.delete(review);
    }

    public List<Review> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProductIdAndIsHiddenFalse(productId);
    }

    public List<ReviewResponseDTO> getReviewDTOsByProductId(Long productId) {
        List<Review> allReviews = getReviewsByProductId(productId);
        // Lấy các review cha (parent == null)
        return allReviews.stream()
                .filter(r -> r.getParent() == null)
                .map(r -> toDTOWithReplies(r, allReviews))
                .collect(Collectors.toList());
    }
    // Thêm bình luận mới
    // ReviewService.java
    public Review addReview(ReviewRequest request, Long userId) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("product", request.getProductId()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setContent(request.getContent());
        review.setRating(request.getRating());
        review.setIsHidden(false);
        if (request.getOrderDetailId() != null) {
            OrderDetail orderDetail = orderDetailRepository.findById(request.getOrderDetailId())
                .orElseThrow(() -> new ResourceNotFoundException("orderDetail", request.getOrderDetailId()));
            review.setOrderDetail(orderDetail);
        }
        Review savedReview = reviewRepository.save(review);
        // Gửi thông báo socket khi có review mới
        webSocketService.broadcastRefresh("review", savedReview, "REVIEW_CREATED");
        return savedReview;
    }

    // Thêm bình luận mới, trả về DTO
    public ReviewResponseDTO addReviewAndReturnDTO(ReviewRequest request, Long userId) {
        Review review = addReview(request, userId);
        // Đã gửi socket trong addReview nên không cần gửi lại ở đây
        return toDTO(review);
    }

    public ReviewResponseDTO toDTO(Review review) {
        return new ReviewResponseDTO(
            review.getId(),
            review.getContent(),
            review.getRating(),
            review.getUser() != null ? review.getUser().getId() : null,
            review.getUser() != null ? review.getUser().getFullname() : null,
            review.getProduct() != null ? review.getProduct().getId() : null,
            review.getCreatedAt() != null ? review.getCreatedAt().toString() : null,
            review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null,
            review.getOrderDetail() != null ? review.getOrderDetail().getId() : null,
            null // replies mặc định null nếu chỉ lấy 1 review đơn lẻ
        );
    }

    // Mapping review kèm replies (dùng cho review cha/con)
    private ReviewResponseDTO toDTOWithReplies(Review review, List<Review> allReviews) {
        List<ReviewResponseDTO> replies = allReviews.stream()
                .filter(r -> r.getParent() != null && r.getParent().getId().equals(review.getId()))
                .map(r -> toDTOWithReplies(r, allReviews))
                .collect(Collectors.toList());
        return new ReviewResponseDTO(
            review.getId(),
            review.getContent(),
            review.getRating(),
            review.getUser() != null ? review.getUser().getId() : null,
            review.getUser() != null ? review.getUser().getFullname() : null,
            review.getProduct() != null ? review.getProduct().getId() : null,
            review.getCreatedAt() != null ? review.getCreatedAt().toString() : null,
            review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null,
            review.getOrderDetail() != null ? review.getOrderDetail().getId() : null,
            replies
        );
    }

    @Transactional
    public ReviewResponseDTO replyToReview(Long parentReviewId, String replyContent, Long adminId) {
        Review parent = getReviewById(parentReviewId);
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new ResourceNotFoundException("admin", adminId));
        Review reply = new Review();
        reply.setContent(replyContent);
        reply.setUser(admin);
        reply.setProduct(parent.getProduct());
        reply.setParent(parent);
        reply.setIsHidden(false);
        reply.setRating(null); // hoặc 0 nếu muốn
        reviewRepository.save(reply);
        return toDTO(reply);
    }

    public List<ReviewInfoDTO> getAllReviewInfo() {
        List<Review> reviews = reviewRepository.findAll();
        return reviews.stream()
            .filter(r -> r.getParent() == null)
            .map(this::toReviewInfoDTO)
            .collect(Collectors.toList());
    }

    public ReviewInfoDTO getReviewInfoById(Long id) {
        Review review = getReviewById(id);
        List<Review> allReviews = reviewRepository.findAll();
        return toReviewInfoDTO(review, allReviews);
    }

    private ReviewInfoDTO toReviewInfoDTO(Review review, List<Review> allReviews) {
        User user = review.getUser();
        Product product = review.getProduct();
        List<ReviewInfoDTO> replies = null;
        if (allReviews != null) {
            replies = allReviews.stream()
                .filter(r -> r.getParent() != null && r.getParent().getId().equals(review.getId()))
                .map(r -> toReviewInfoDTO(r, null))
                .collect(Collectors.toList());
        }
        return new ReviewInfoDTO(
            review.getId(),
            user != null ? user.getFullname() : null,
            user != null ? user.getPhoneNumber() : null,
            user != null ? user.getEmail() : null,
            product != null ? product.getId() : null,
            product != null ? product.getName() : null,
            review.getRating(),
            review.getContent(),
            review.getCreatedAt() != null ? review.getCreatedAt().toString() : null,
            review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null,
            review.getIsHidden(),
            replies
        );
    }

    private ReviewInfoDTO toReviewInfoDTO(Review review) {
        return toReviewInfoDTO(review, null);
    }
}