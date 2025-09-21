package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.AdminCreateUserRequest;
import com.example.shoozy_shop.dto.request.AdminUpdateUserRequest;
import com.example.shoozy_shop.dto.response.UserDto;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.User;
import com.example.shoozy_shop.repository.UserRepository;
import com.example.shoozy_shop.service.UserService;

import jakarta.validation.Valid;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Controller dành cho Admin/Staff:
 * - Admin có thể xem danh sách tất cả user và thay đổi status/role
 * - Staff chỉ được phép xem danh sách “Customer” và xem chi tiết user bất kỳ
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasAnyAuthority('Staff', 'Admin')")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Page<UserDto>>> getUsers(
            @RequestParam(value = "role", required = false) String roleName,
            @RequestParam(value = "status", required = false) Boolean status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = authentication.getAuthorities().iterator().next().getAuthority();

        if ("Staff".equals(currentRole)) {
            if (roleName == null || !"Customer".equalsIgnoreCase(roleName)) {
                throw new AccessDeniedException("Bạn không có quyền xem danh sách này");
            }
        }

        Page<UserDto> page = userService.searchUsers(roleName, status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", page));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable("id") Long userId) {
        UserDto dto = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", dto));
    }

    @PatchMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserDto>> changeStatus(
            @PathVariable("id") Long userId,
            @RequestParam("active") Boolean isActive) {
        UserDto dto = userService.changeUserStatus(userId, isActive);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", dto));
    }

    @PatchMapping(value = "/{id}/role", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserDto>> changeRole(
            @PathVariable("id") Long userId,
            @RequestParam("roleName") String roleName) {
        UserDto dto = userService.changeUserRole(userId, roleName);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò thành công", dto));
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @ModelAttribute AdminCreateUserRequest req) {
        UserDto created = userService.createUser(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo người dùng thành công", created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute AdminUpdateUserRequest req) {
        UserDto updated = userService.updateUser(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", updated));
    }

    @PutMapping("/{id}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (!user.getIsActive()) {
            return ResponseEntity
                    .badRequest()
                    .body("Tài khoản đã bị khoá rồi.");
        }

        userService.deleteUser(id); // Xoá mềm (set isActive = false)
        return ResponseEntity.ok("Đã khoá tài khoản người dùng");
    }

}
