package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.Brand;
import com.example.shoozy_shop.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<Brand>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.success("Lấy tất cả thương hiệu thành công", brandService.getAllBrands()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Brand>>> getActiveBrands() {
        return ResponseEntity.ok(ApiResponse.success("Lấy thương hiệu đang hoạt động", brandService.getActiveBrands()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Brand>> getBrandById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thương hiệu theo ID thành công", brandService.getBrandById(id)));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<Brand>> createBrand(@RequestBody @Valid Brand brand) {
        return ResponseEntity.ok(ApiResponse.success("Tạo thương hiệu thành công", brandService.addBrand(brand)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Brand>> updateBrand(@PathVariable Long id, @RequestBody @Valid Brand brand) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thương hiệu thành công", brandService.updateBrand(id, brand)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thương hiệu thành công", null));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        boolean restored = brandService.restoreBrand(id);
        if (restored) {
            return ResponseEntity.ok("Brand restored successfully");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Brand not found");
    }

}