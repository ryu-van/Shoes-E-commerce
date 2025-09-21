package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.Size;
import com.example.shoozy_shop.service.SizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/sizes")
public class SizeController {

    private final SizeService sizeService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<Size>>> getAllSizes() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách size thành công", sizeService.getAllSizes()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Size>>> getActiveSizes() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách size đang hoạt động thành công", sizeService.getActiveSizes()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Size>> getSizeById(@PathVariable Long id) {
        Size size = sizeService.getSizeById(id);
        if (size == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Không tìm thấy size với id = " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy size thành công", size));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<Size>> createSize(@RequestBody @Valid Size size) {
        return ResponseEntity.ok(ApiResponse.success("Tạo size thành công", sizeService.addSize(size)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Size>> updateSize(@PathVariable Long id, @RequestBody @Valid Size size) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật size thành công", sizeService.updateSize(id, size)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSize(@PathVariable Long id) {
        sizeService.deleteSize(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá size thành công với id = " + id, null));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        boolean restored = sizeService.restoreBrand(id);
        if (restored) {
            return ResponseEntity.ok("Size restored successfully");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Size not found");
    }

}