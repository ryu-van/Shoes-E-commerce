package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.ProductVariantAddRequest;
import com.example.shoozy_shop.dto.request.ProductVariantUpdateRequest;
import com.example.shoozy_shop.dto.response.ProductVariantResponse;
import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.*;
import com.example.shoozy_shop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVariantService implements IProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;
    private final MaterialRepository materialRepository;
    private final ColorRepository colorRepository;

    @Override
    public List<ProductVariantResponse> getAllProductVariants() {
        List<ProductVariant> list = productVariantRepository.findAll();
        return list.stream()
                .map(ProductVariantResponse::fromProductVariant)
                .collect(Collectors.toList());
    }

    @Override
    public ProductVariantResponse getProductVariantById(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product variant", id));

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct().getId())
                .color(variant.getColor())
                .size(variant.getSize())
                .costPrice(variant.getCostPrice())
                .sellPrice(variant.getSellPrice())
                .quantity(variant.getQuantity())
                .build();
    }
    @Override
    public ProductVariant addProductVariant(ProductVariantAddRequest productVariantAddRequest) {
        Product existingProduct = productRepository.findById(productVariantAddRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("product", productVariantAddRequest.getProductId()));
        Size existingSize = sizeRepository.findById(productVariantAddRequest.getSizeId())
                .orElseThrow(() -> new ResourceNotFoundException("size", productVariantAddRequest.getSizeId()));
        Color existingColor = colorRepository.findById(productVariantAddRequest.getColorId())
                .orElseThrow(() -> new ResourceNotFoundException("color", productVariantAddRequest.getColorId()));
        ProductVariant newProductVariant = ProductVariant.builder()
                .product(existingProduct)
                .size(existingSize)
                .color(existingColor)
                .quantity(productVariantAddRequest.getQuantity())
                .costPrice(productVariantAddRequest.getCostPrice())
                .sellPrice(productVariantAddRequest.getSellPrice())
                .build();
        return productVariantRepository.save(newProductVariant);
    }

    @Override
    public ProductVariant updateProductVariant(Long id, ProductVariantUpdateRequest productVariantUpdateRequest) {
        ProductVariant existingProductVariant = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product variant", id));
        existingProductVariant.setQuantity(productVariantUpdateRequest.getQuantity());
        existingProductVariant.setCostPrice(productVariantUpdateRequest.getCostPrice());
        existingProductVariant.setSellPrice(productVariantUpdateRequest.getSellPrice());
        return productVariantRepository.save(existingProductVariant);
    }

    @Override
    public void deleteProductVariant(Long id) {
        ProductVariant existingProductVariant = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("product variant", id));
        existingProductVariant.setStatus(false);
        productVariantRepository.save(existingProductVariant);
    }
}
