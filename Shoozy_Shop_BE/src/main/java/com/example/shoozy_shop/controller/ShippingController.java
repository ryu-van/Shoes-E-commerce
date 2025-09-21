package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.OpenApiRequest;
import com.example.shoozy_shop.dto.response.FeeShippingResponse;
import com.example.shoozy_shop.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/shipping")
public class ShippingController {
    private final ShippingService shipping;

    // Nhận địa chỉ từ Province API (code + name đều được)
    @PostMapping("/fee-from-openapi")
    public ResponseEntity<FeeShippingResponse> feeFromOpenApi(@RequestBody OpenApiRequest req) {
        return ResponseEntity.ok(shipping.calcFromProvinceAddress(req));
    }
}
