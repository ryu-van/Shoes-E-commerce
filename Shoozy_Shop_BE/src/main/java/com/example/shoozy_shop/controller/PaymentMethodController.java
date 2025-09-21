package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.response.ApiResponse;
import com.example.shoozy_shop.dto.response.PaymentMethodResponse;
import com.example.shoozy_shop.model.PaymentMethod;
import com.example.shoozy_shop.service.IPaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/payment-methods")
public class PaymentMethodController {

    private final IPaymentMethodService paymentMethodService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllPaymentMethods() {
        List<PaymentMethod> paymentMethods = paymentMethodService.getAllPaymentMethods();
        List<PaymentMethodResponse> paymentMethodResponses = paymentMethods.stream()
                .map(PaymentMethodResponse::fromModel)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Get all payment methods successfully", paymentMethodResponses));
    }
}
