package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.ProductRequest;
import com.example.shoozy_shop.dto.response.ListProductPromotion;
import com.example.shoozy_shop.dto.response.ProductPromotionResponse;
import com.example.shoozy_shop.dto.response.ProductResponse;
import com.example.shoozy_shop.dto.response.ProductResponseForPromotion;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.Product;
import com.example.shoozy_shop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "brand_id", required = false) List<Long> brandIds,
            @RequestParam(name = "category_id", required = false) List<Long> categoryIds,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @RequestParam(name = "size_id", required = false) List<Long> sizeIds,
            @RequestParam(name = "color_id", required = false) List<Long> colorIds,
            @RequestParam(name = "material_id", required = false) List<Long> materialIds,
            @RequestParam(name = "gender", required = false) String gender,
            @RequestParam(name = "status", required = false) Boolean status,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "asc") String sortDirection
    ) {
        Page<ProductResponse> results = productService.searchProductByQueryBuilder(
                keyword, brandIds, categoryIds, minPrice, maxPrice,
                sizeIds, colorIds, materialIds, gender, status, pageNo, pageSize, sortBy, sortDirection
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sản phẩm thành công", productService.getProductById(id)));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody @Valid ProductRequest productRequest) {
        return ResponseEntity.ok(ApiResponse.success("Tạo sản phẩm thành công", productService.addProduct(productRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(@PathVariable Long id, @RequestBody @Valid ProductRequest productRequest) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", productService.updateProduct(id, productRequest)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }

    @GetMapping("/check-name")
    public ResponseEntity<ApiResponse<Boolean>> checkProductName(@RequestParam String name) {
        boolean exists = productService.existsProductByName(name);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra tên sản phẩm", exists));
    }

    @GetMapping("/promotion/products-by-ids")
    public ResponseEntity<ApiResponse<List<ProductResponseForPromotion>>> getProductsForPromotion(@RequestParam List<Long> ids) {
        List<ProductResponseForPromotion> result = productService.getAllActiveProductsForPromotion(ids);
        return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm khuyến mãi theo ID thành công", result));
    }

    @GetMapping("/by-category")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProductsByCategory(
            @RequestParam Long categoryId,
            @RequestParam Long productId) {
        List<ProductResponse> products = productService.getProductsByCategoryId(categoryId, productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm theo danh mục thành công", products));
    }

    @GetMapping("/product-promotion")
    public ResponseEntity<?> getProductPromotionPage(
            @RequestParam(name = "productName", required = false) String productName,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "15") Integer pageSize
    ) {
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize);
        Page<ProductPromotionResponse> productPage = productService.getProductPromotionPage(productName, categoryId, pageRequest);
        List<ProductPromotionResponse> productList = productPage.getContent();
        int totalPage = productPage.getTotalPages();
        int totalElements = (int) productPage.getTotalElements();
        ListProductPromotion listProductPromotion = ListProductPromotion.builder()
                .productPromotions(productList)
                .totalPage(totalPage)
                .totalElements(totalElements)
                .build();
        return ResponseEntity.ok(listProductPromotion);
    }

    @GetMapping("/active-for-promotion")
    public ResponseEntity<List<ProductResponseForPromotion>> getAllActiveProductsForPromotion(
            @RequestParam("productIds") List<Long> productIds
    ) {
        List<ProductResponseForPromotion> products = productService.getAllActiveProductsForPromotion(productIds);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        boolean restored = productService.restoreProduct(id);
        if (restored) {
            return ResponseEntity.ok("Product restored successfully");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
    }

}

