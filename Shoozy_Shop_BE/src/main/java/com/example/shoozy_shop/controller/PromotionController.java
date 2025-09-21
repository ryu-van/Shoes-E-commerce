package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.PromotionRequest;
import com.example.shoozy_shop.dto.response.ListPromotionResponse;
import com.example.shoozy_shop.dto.response.PromotionDetailResponse;
import com.example.shoozy_shop.dto.response.PromotionResponse;
import com.example.shoozy_shop.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.shoozy_shop.exception.ApiResponse;

import java.time.LocalDate;

@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/promotions")
public class PromotionController {
    private final PromotionService promotionService;

    @GetMapping("")
    public ResponseEntity<?> getPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer status
    ) throws Exception {
        PageRequest pageRequest = PageRequest.of(page, limit);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            LocalDate t = startDate; startDate = endDate; endDate = t;
        }

        Page<PromotionResponse> promotionResponses =
                promotionService.getPromotionByPage(pageRequest,
                        (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                        startDate, endDate, status);

        ListPromotionResponse response = ListPromotionResponse.builder()
                .promotions(promotionResponses.getContent())
                .totalPage(promotionResponses.getTotalPages())
                .totalElements((int) promotionResponses.getTotalElements())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Get promotions successfully", response));
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getPromotionById(@PathVariable("id") Long id) throws Exception {
        PromotionDetailResponse promotionResponse = promotionService.getPromotionInfoById(id);
        return ResponseEntity.ok(ApiResponse.success("Get promotion successfully", promotionResponse));
    }
    @PutMapping("/status/{id}")
    public ResponseEntity<?> updatePromotionStatus(@PathVariable("id") Long id) throws Exception {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.success("Update promotion status successfully", null));
    }

    @PostMapping("")
    public ResponseEntity<?> createPromotion(@RequestBody PromotionRequest promotionRequest) throws Exception {
        PromotionResponse promotionResponse = promotionService.createPromotion(promotionRequest);
        return ResponseEntity.ok(ApiResponse.success("Create promotion successfully", promotionResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePromotion(@PathVariable("id") Long id, @RequestBody PromotionRequest promotionRequest) throws Exception {
        PromotionResponse promotionResponse = promotionService.updatePromotion(id, promotionRequest);
        return ResponseEntity.ok(ApiResponse.success("Update promotion successfully", promotionResponse));
    }
    @PutMapping("/active/{id}")
    public ResponseEntity<?> activePromotion(@PathVariable("id") Long id) throws Exception {
        promotionService.activePromotion(id);
        return ResponseEntity.ok(ApiResponse.success("Active promotion successfully", null));
    }
}
