package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.response.ProductVariantImageResponse;
import com.example.shoozy_shop.model.ProductVariantImage;

import java.util.List;
import java.util.Map;

public interface IProductVariantImageService {
    ProductVariantImage addProductVariantImage(Long productVariantId, Long imageId);
    Map<Long, List<String>> getGroupedImagesByVariantIds(List<Long> variantIds);
}
