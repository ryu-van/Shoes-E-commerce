package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.PromotionRequest;
import com.example.shoozy_shop.dto.response.PromotionDetailResponse;
import com.example.shoozy_shop.dto.response.PromotionResponse;
import com.example.shoozy_shop.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public interface IPromotionService {
    PromotionDetailResponse getPromotionInfoById(Long id) throws Exception;
    Promotion getPromotionById(Long id) throws Exception;
    Page<PromotionResponse> getPromotionByPage(Pageable pageable, String keyword, LocalDate dateBefore, LocalDate dateAfter, Integer status) throws Exception;
    void deletePromotion(Long id) throws Exception;
    PromotionResponse updatePromotion(Long id, PromotionRequest promotionRequest) throws Exception;
    PromotionResponse createPromotion(PromotionRequest promotionRequest) throws Exception;
    void activePromotion(Long id) throws Exception;
}
