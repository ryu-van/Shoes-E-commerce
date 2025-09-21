package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.ProductVariantAddRequest;
import com.example.shoozy_shop.dto.request.ProductVariantUpdateRequest;
import com.example.shoozy_shop.dto.response.ProductVariantResponse;
import com.example.shoozy_shop.model.ProductVariant;

import java.util.List;

public interface IProductVariantService {
    List<ProductVariantResponse> getAllProductVariants();
    ProductVariantResponse getProductVariantById(Long id);
    ProductVariant addProductVariant(ProductVariantAddRequest productVariantRequest);
    ProductVariant updateProductVariant(Long id, ProductVariantUpdateRequest productVariantUpdateRequest);
    void deleteProductVariant(Long id);
}
