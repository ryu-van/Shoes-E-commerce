package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.ProductRequest;
import com.example.shoozy_shop.dto.response.*;
import com.example.shoozy_shop.model.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IProducService {
    // List<ProductResponse> getAllProducts();
    Page<ProductResponse> searchProductByQueryBuilder(String keyword, List<Long> brandIds, List<Long> categoryIds,
                                                      Double minPrice, Double maxPrice, List<Long> sizeIds, List<Long> colorIds, List<Long> materialIds,
                                                      String gender, Boolean status, Integer pageNo, Integer pageSize, String sortBy, String sortDirection);
    ProductResponse getProductById(Long id);
    Product addProduct(ProductRequest productRequest);
    Product updateProduct(Long id, ProductRequest productRequest);
    void deleteProduct(Long id);
    boolean existsProductByName(String name);
    List<ProductResponseForPromotion> getAllActiveProductsForPromotion(List<Long> productIds);
    List<ProductResponse> getProductsByCategoryId(Long categoryId, Long productId);
    List<TopSellingProductResponse> getTopSellingProducts(String filterType);
    List<LowStockProductResponse> findLowStockProducts();
    Page<ProductPromotionResponse> getProductPromotionPage(String productName, Long categoryId, Pageable pageable);
    Boolean restoreProduct(Long id);
}
