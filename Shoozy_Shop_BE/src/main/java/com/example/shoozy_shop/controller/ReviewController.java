package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.model.Review;
import com.example.shoozy_shop.dto.request.ReviewRequest;
import com.example.shoozy_shop.dto.response.ReviewResponseDTO;
import com.example.shoozy_shop.dto.response.ReviewInfoDTO;

import com.example.shoozy_shop.service.ReviewService;
import com.example.shoozy_shop.repository.UserRepository;
import com.example.shoozy_shop.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/reviews")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    // Lấy tất cả review
    @GetMapping("")
    public ResponseEntity<List<Review>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    // Lấy review theo ID
    @GetMapping("/{id}")
    public ResponseEntity<Review> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    // Cập nhật review
    @PutMapping("/{id}")
    public ResponseEntity<Review> updateReview(@PathVariable Long id, @RequestBody Review review) {
        return ResponseEntity.ok(reviewService.updateReview(id, review));
    }
    // Thêm bình luận mới
    @PostMapping("")
    public ResponseEntity<List<ReviewResponseDTO>> addReview(@RequestBody ReviewRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
        reviewService.addReview(request, user.getId());
        List<ReviewResponseDTO> dtos = reviewService.getReviewDTOsByProductId(request.getProductId());
        return ResponseEntity.ok(dtos);
    }
    // Lấy review theo productId
    @GetMapping("/by-product/{productId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByProductId(@PathVariable Long productId) {
        List<ReviewResponseDTO> dtos = reviewService.getReviewDTOsByProductId(productId);
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/reviews/{id}/reply")
    public ReviewResponseDTO replyToReview(@PathVariable Long id, @RequestBody String replyContent, @RequestParam Long adminId) {
        return reviewService.replyToReview(id, replyContent, adminId);
    }

    @GetMapping("/info")
    public ResponseEntity<List<ReviewInfoDTO>> getAllReviewInfo() {
        return ResponseEntity.ok(reviewService.getAllReviewInfo());
    }

    @GetMapping("/info/{id}")
    public ResponseEntity<ReviewInfoDTO> getReviewInfoById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewInfoById(id));
    }
}