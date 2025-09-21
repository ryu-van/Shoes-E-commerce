package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.ProductVariantAddRequest;
import com.example.shoozy_shop.dto.request.ProductVariantUpdateRequest;
import com.example.shoozy_shop.dto.response.ProductVariantResponse;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.ProductVariant;
import com.example.shoozy_shop.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/product-variants")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getAllProductVariants() {
        List<ProductVariantResponse> list = productVariantService.getAllProductVariants();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách biến thể sản phẩm thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getProductVariantById(@PathVariable Long id) {
        ProductVariantResponse variant = productVariantService.getProductVariantById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy biến thể sản phẩm thành công", variant));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<ProductVariant>> addProductVariant(
            @RequestBody @Valid ProductVariantAddRequest request) {
        ProductVariant result = productVariantService.addProductVariant(request);
        return ResponseEntity.ok(ApiResponse.success("Thêm biến thể sản phẩm thành công", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductVariant>> updateProductVariant(
            @PathVariable Long id,
            @RequestBody @Valid ProductVariantUpdateRequest request) {
        ProductVariant updated = productVariantService.updateProductVariant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật biến thể sản phẩm thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProductVariant(@PathVariable Long id) {
        productVariantService.deleteProductVariant(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa biến thể sản phẩm thành công", null));
    }
}