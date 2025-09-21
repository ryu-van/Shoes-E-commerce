package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.ChangePasswordRequest;
import com.example.shoozy_shop.dto.request.UpdateUserRequest;
import com.example.shoozy_shop.dto.response.UserDto;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.service.MinioService;
import com.example.shoozy_shop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MinioService minioService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserDto dto = userService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", dto));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request) {

        String email = authentication.getName();
        UserDto updated = userService.updateProfile(email, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", updated));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<?>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        String email = authentication.getName();
        userService.changePassword(email, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công. Vui lòng đăng nhập lại.", null));
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = minioService.uploadUserAvatar(file);
        return ResponseEntity.ok(ApiResponse.success("Upload avatar thành công", avatarUrl));
    }

    @PutMapping("/avatar/{userId}")
    public ResponseEntity<ApiResponse<?>> updateUserAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile avatarFile) {
        try {
            // Validate
            if (avatarFile.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "File không được để trống"));
            }

            // Use Tika to detect actual file type
            String detectedType = new Tika().detect(avatarFile.getInputStream());
            if (!detectedType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "File phải là ảnh"));
            }

            if (avatarFile.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "File không được vượt quá 5MB"));
            }

            String avatarUrl = userService.updateUserAvatarById(userId, avatarFile);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật avatar thành công", avatarUrl));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "Lỗi server khi upload avatar"));
        }
    }
}