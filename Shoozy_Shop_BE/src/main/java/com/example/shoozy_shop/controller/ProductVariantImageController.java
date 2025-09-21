package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.response.ProductImageByColorResponse;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.ProductImage;
import com.example.shoozy_shop.model.ProductVariantImage;
import com.example.shoozy_shop.service.ProductImageService;
import com.example.shoozy_shop.service.ProductVariantImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/product-variant-images")
public class ProductVariantImageController {

    private final ProductVariantImageService productVariantImageService;
    private final ProductImageService productImageService;

    @GetMapping("/grouped-images-by-ids/{ids}")
    public ResponseEntity<ApiResponse<Map<Long, List<String>>>> getGroupedImagesByVariantIdsFromPath(
            @PathVariable("ids") String ids) {
        List<Long> variantIds = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        Map<Long, List<String>> grouped = productVariantImageService.getGroupedImagesByVariantIds(variantIds);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ảnh theo variant thành công", grouped));
    }

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ProductVariantImage>>> uploadImagesForVariants(
            @RequestParam("imageIds") String imageIds,
            @RequestParam("variantIds") List<Long> variantIds
    ) {
        List<ProductVariantImage> result = new ArrayList<>();

        // 1. Lấy các ảnh từ imageIds
        List<Long> ids = Arrays.stream(imageIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();

        List<ProductImage> imagesToAssign = ids.stream()
                .map(productImageService::getById)
                .collect(Collectors.toList());

        // 2. Gán ảnh cho từng variant
        for (Long variantId : variantIds) {
            for (ProductImage image : imagesToAssign) {
                ProductVariantImage pvi = productVariantImageService.addProductVariantImage(variantId, image.getId());
                result.add(pvi);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Gán ảnh thành công", result));
    }

    @PostMapping(value = "/upload-only", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ProductImage>>> uploadImage(
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Không có file nào được gửi lên"));
        }

        List<ProductImage> savedImages = new ArrayList<>();
        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(ApiResponse.error(415, "File không đúng định dạng ảnh"));
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(ApiResponse.error(413, "Kích thước ảnh vượt quá 5MB"));
            }

            ProductImage saved = productImageService.addProductImages(file);
            savedImages.add(saved);
        }

        return ResponseEntity.ok(ApiResponse.success("Tải ảnh lên thành công", savedImages));
    }

    @GetMapping("/by-color")
    public ResponseEntity<ApiResponse<List<ProductImageByColorResponse>>> getImagesByColor(@RequestParam Long colorId) {
        List<ProductImageByColorResponse> list = productImageService.getImagesByColor(colorId);
        return ResponseEntity.ok(ApiResponse.success("Lấy ảnh theo màu thành công", list));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteByImageId(@PathVariable("id") Long imageId) {
        productVariantImageService.deleteByImageId(imageId);
        return ResponseEntity.ok(ApiResponse.success("Xóa ảnh thành công", null));
    }
}