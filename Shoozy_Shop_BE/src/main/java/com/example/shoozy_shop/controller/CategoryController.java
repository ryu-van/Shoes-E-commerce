package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.Category;
import com.example.shoozy_shop.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success("Lấy tất cả danh mục thành công", categoryService.getAllCategories()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Category>>> getActiveCategories() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục đang hoạt động", categoryService.getActiveCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục theo ID thành công", categoryService.getCategoryById(id)));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody @Valid Category category) {
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục thành công", categoryService.addCategory(category)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(@PathVariable Long id, @RequestBody @Valid Category category) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", categoryService.updateCategory(id, category)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        boolean restored = categoryService.restoreCategory(id);
        if (restored) {
            return ResponseEntity.ok("Category restored successfully");
        } else {
            return ResponseEntity.status(404).body("Category not found or not deleted");
        }
    }
}