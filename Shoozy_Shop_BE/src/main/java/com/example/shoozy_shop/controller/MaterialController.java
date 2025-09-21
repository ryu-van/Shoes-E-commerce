package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.Material;
import com.example.shoozy_shop.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/materials")
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<Material>>> getAllMaterials() {
        List<Material> materials = materialService.getAllMaterials();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chất liệu thành công", materials));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Material>>> getActiveMaterials() {
        List<Material> materials = materialService.getActiveMaterials();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chất liệu đang hoạt động", materials));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Material>> getMaterialById(@PathVariable Long id) {
        Material material = materialService.getMaterialById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chất liệu theo ID thành công", material));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<Material>> createMaterial(@RequestBody @Valid Material material) {
        Material created = materialService.addMaterial(material);
        return ResponseEntity.ok(ApiResponse.success("Tạo chất liệu thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Material>> updateMaterial(@PathVariable Long id, @RequestBody @Valid Material material) {
        Material updated = materialService.updateMaterial(id, material);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chất liệu thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa chất liệu thành công", null));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        boolean restored = materialService.restoreMaterial(id);
        if (restored) {
            return ResponseEntity.ok("Material restored successfully");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Material not found");
    }

}