package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.CreateReturnRequestRequest;
import com.example.shoozy_shop.dto.response.ReturnRequestResponse;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.ReturnRequest;
import com.example.shoozy_shop.service.MinioService;
import com.example.shoozy_shop.service.ReturnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.shoozy_shop.security.UserDetailsImpl;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {
    @Autowired
    private final ReturnService returnService;

    @Autowired
    private final MinioService minioService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createReturnRequest(
            @RequestBody CreateReturnRequestRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long userId = userDetails.getId();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(request);
            System.out.println("Received return request JSON: " + json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ReturnRequest returnRequest = returnService.createReturnRequest(request, userId);
        ReturnRequestResponse response = returnService.convertToDto(returnRequest);
        return ResponseEntity.ok(ApiResponse.success("Tạo yêu cầu trả hàng thành công", response));
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<ReturnRequestResponse>>> getReturnsByUserId(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String q, // mã đơn hoặc tên sản phẩm
            @RequestParam(required = false) String status // PENDING/APPROVED/...
    ) {
        Long userId = userDetails.getId();
        List<ReturnRequestResponse> responses = returnService.getReturnsByUserId(userId, q, status);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử trả hàng thành công", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> getReturnRequestDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        ReturnRequest returnRequest = returnService.getReturnRequestByIdAndUser(id, userDetails.getId());
        ReturnRequestResponse response = returnService.convertToDto(returnRequest);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết trả hàng thành công", response));
    }

    @PostMapping(value = "/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadReturnImages(
            @RequestParam("files") List<MultipartFile> files) {

        try {
            // (tuỳ chọn) check nhanh ở controller — Service vẫn validate lại lần nữa
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Vui lòng chọn ít nhất 1 ảnh."));
            }
            if (files.size() > 5) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Chỉ cho phép tối đa 5 ảnh."));
            }

            List<String> urls = minioService.uploadReturnImages(files); // đã validate định dạng + kích thước + số lượng
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công", urls));

        } catch (IllegalArgumentException e) { // sai định dạng, >5MB/ảnh, quá 5 ảnh, ảnh rỗng, v.v.
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Upload ảnh thất bại"));
        }
    }

}
