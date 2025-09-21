package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.response.ProductVariantImageResponse;
import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Product;
import com.example.shoozy_shop.model.ProductImage;
import com.example.shoozy_shop.model.ProductVariant;
import com.example.shoozy_shop.model.ProductVariantImage;
import com.example.shoozy_shop.repository.ProductImageRepository;
import com.example.shoozy_shop.repository.ProductVariantImageRepository;
import com.example.shoozy_shop.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductVariantImageService implements IProductVariantImageService {

    private final ProductVariantImageRepository productVariantImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public ProductVariantImage addProductVariantImage(Long productVariantId, Long imageId) {
        ProductVariant variant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException("product variant", productVariantId));
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("product image", imageId));

        // Kiểm tra nếu đã tồn tại bản ghi thì không thêm mới
        Optional<ProductVariantImage> existing = productVariantImageRepository
                .findByProductVariantIdAndProductImageId(productVariantId, imageId);
        if (existing.isPresent()) {
            return existing.get();
        }

        ProductVariantImage productVariantImage = ProductVariantImage.builder()
                .productVariant(variant)
                .productImage(image)
                .build();

        // Set thumbnail cho biến thể nếu chưa có
        if (variant.getThumbnail() == null || variant.getThumbnail().isBlank()) {
            variant.setThumbnail(image.getImageUrl());
        }

        // Set thumbnail cho sản phẩm nếu chưa có
        Product product = variant.getProduct();
        if (product.getThumbnail() == null || product.getThumbnail().isBlank() || product.getThumbnail().equals("http://localhost:9000/product-variant-images/%E3%83%8E%E3%83%BC%E3%82%A4%E3%83%A1%E3%83%BC%E3%82%B7%E3%82%99-760x460.png")) {
            List<ProductVariant> variants = product.getProductVariants();
            if (!variants.isEmpty()) {
                ProductVariant firstVariant = variants.get(0);
                if (firstVariant.getThumbnail() != null) {
                    product.setThumbnail(firstVariant.getThumbnail());
                }
            }
        }

        return productVariantImageRepository.save(productVariantImage);
    }


    @Override
    public Map<Long, List<String>> getGroupedImagesByVariantIds(List<Long> variantIds) {
        List<ProductVariantImageResponse> responseList = productVariantImageRepository.findImagesByVariantIds(variantIds);
        return responseList.stream()
                .collect(Collectors.groupingBy(
                        ProductVariantImageResponse::getVariantId,
                        Collectors.mapping(ProductVariantImageResponse::getImageUrl, Collectors.toList())
                ));
    }

    public void deleteByImageId(Long imageId) {
        productVariantImageRepository.deleteById(imageId);
    }
}
