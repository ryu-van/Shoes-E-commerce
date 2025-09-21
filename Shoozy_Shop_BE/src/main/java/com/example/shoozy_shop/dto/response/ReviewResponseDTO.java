package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {
    private Long id;
    private String content;
    private Integer rating;
    private Long userId;
    private String userFullname;
    private Long productId;
    private String createdAt;
    private String updatedAt;
    private Long orderDetailId;
    private List<ReviewResponseDTO> replies;
} 