package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.Color;
import com.example.shoozy_shop.service.ColorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/colors")
public class ColorController {

    private final ColorService colorService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<Color>>> getAllColors() {
        List<Color> colors = colorService.getAllColors();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách màu thành công", colors));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Color>>> getActiveColors() {
        List<Color> colors = colorService.getActiveColors();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách màu đang hoạt động", colors));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Color>> getColorById(@PathVariable Long id) {
        Color color = colorService.getColorById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy màu theo ID thành công", color));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<Color>> createColor(@RequestBody @Valid Color color) {
        Color created = colorService.addColor(color);
        return ResponseEntity.ok(ApiResponse.success("Tạo màu thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Color>> updateColor(@PathVariable Long id, @RequestBody @Valid Color color) {
        Color updated = colorService.updateColor(id, color);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật màu thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteColor(@PathVariable Long id) {
        colorService.deleteColor(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa màu thành công", null));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        boolean restored = colorService.restoreColor(id);
        if (restored) {
            return ResponseEntity.ok("Color restored successfully");
        } else {
            return ResponseEntity.status(404).body("Color not found or not deleted");
        }
    }
}