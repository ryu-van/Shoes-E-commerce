package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.response.ProductImageByColorResponse;
import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.ProductImage;
import com.example.shoozy_shop.repository.ProductImageRepository;
import com.example.shoozy_shop.repository.ProductVariantImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductImageService implements IProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductVariantImageRepository productVariantImageRepository;
    private final MinioService minioService;

    @Override
    public ProductImage addProductImages(MultipartFile file) {
        String imageUrl = minioService.uploadProductImages(file);
        ProductImage productImage = ProductImage.builder()
                .imageUrl(imageUrl)
                .build();
        return productImageRepository.save(productImage);
    }

    @Override
    public ProductImage getById(Long id) {
        return productImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product image", id));
    }

    @Override
    public List<ProductImageByColorResponse> getImagesByColor(Long colorId) {
        return productImageRepository.getImagesByColor(colorId);
    }

}
