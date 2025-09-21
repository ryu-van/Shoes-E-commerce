package com.example.shoozy_shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewInfoDTO {
    private Long id;
    private String userFullName;
    private String userPhone;
    private String userEmail;
    private Long productId;
    private String productName;
    private Integer rating;
    private String content;
    private String createdAt;
    private String updatedAt;
    private Boolean isHidden;
    private List<ReviewInfoDTO> replies;
} 