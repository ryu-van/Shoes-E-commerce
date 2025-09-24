package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.UpdateReturnStatusRequest;
import com.example.shoozy_shop.dto.response.ReturnRequestResponse;
import com.example.shoozy_shop.enums.ReturnStatus;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.service.ReturnService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/returns/admin")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('Admin') or hasAuthority('Staff')")
public class AdminReturnRequestController {

    @Autowired
    private final ReturnService returnService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllReturnRequests(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false) String status) {
        Page<ReturnRequestResponse> results = returnService.getAllForAdmin(page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu trả hàng thành công", results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getDetail(@PathVariable Long id) {
        ReturnRequestResponse response = returnService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết yêu cầu trả hàng thành công", response));
    }

    // com/example/shoozy_shop/controller/AdminReturnRequestController.java
    @PostMapping("/update-status")
    public ResponseEntity<ApiResponse<?>> updateReturnStatus(
            @RequestBody @Valid UpdateReturnStatusRequest requestDto) {

        if (requestDto.getStatus() == ReturnStatus.REFUNDED) {
            // gọi overload mới – hiện thời chỉ chuyển tiếp, Bước 4 sẽ thêm xử lý hoàn tiền
            // thật
            returnService.updateStatus(
                    requestDto.getReturnRequestId(),
                    requestDto.getStatus(),
                    requestDto.getRefundMethod(),
                    requestDto.getReferenceCode(),
                    requestDto.getRefundNote());
        } else {
            // giữ nguyên cho các status khác
            returnService.updateStatus(requestDto.getReturnRequestId(), requestDto.getStatus().name());
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật trạng thái yêu cầu trả hàng thành công", null));
    }

}
