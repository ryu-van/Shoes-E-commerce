package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.OrderTimelineRequest;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.service.OrderTimelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/order-timeline")
@RequiredArgsConstructor
public class OrderTimelineController {

    private final OrderTimelineService orderTimelineService;

    @GetMapping("")
    public ResponseEntity<?> getOrderTimelinesByOrderId(@RequestParam("orderId") Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy timeline đơn hàng thành công",
                orderTimelineService.getOrderTimelinesByOrderId(orderId))
        );
    }

    @PostMapping("")
    public ResponseEntity<?> createTimeline(@Valid @RequestBody OrderTimelineRequest orderTimelineRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo timeline đơn hàng thành công",
                orderTimelineService.createOrderTimeline(orderTimelineRequest))
        );
    }

}
