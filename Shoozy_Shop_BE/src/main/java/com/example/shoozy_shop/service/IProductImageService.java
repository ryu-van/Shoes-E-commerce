package com.example.shoozy_shop.service;


import com.example.shoozy_shop.dto.response.ProductImageByColorResponse;
import com.example.shoozy_shop.model.ProductImage;
import com.example.shoozy_shop.model.ProductVariantImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IProductImageService {
    ProductImage addProductImages(MultipartFile file);
    ProductImage getById(Long id);
    List<ProductImageByColorResponse> getImagesByColor(Long colorId);
}
