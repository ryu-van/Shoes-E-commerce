package com.example.shoozy_shop.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.example.shoozy_shop.dto.request.CreateReturnRequestRequest;
import com.example.shoozy_shop.dto.response.ReturnRequestResponse;
import com.example.shoozy_shop.model.ReturnRequest;

public interface ReturnService {
    ReturnRequest createReturnRequest(CreateReturnRequestRequest request, Long userId);

    ReturnRequestResponse convertToDto(ReturnRequest returnRequest);

    // ReturnService.java
    List<ReturnRequestResponse> getReturnsByUserId(Long userId, String q, String status);

    ReturnRequest getReturnRequestByIdAndUser(Long id, Long userId);

    Page<ReturnRequestResponse> getAllForAdmin(int page, int size, String status);

    void updateStatus(Long returnRequestId, String statusStr);

    ReturnRequestResponse getById(Long returnRequestId);

}
