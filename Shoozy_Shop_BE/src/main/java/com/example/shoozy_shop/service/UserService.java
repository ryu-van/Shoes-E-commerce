package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.AdminCreateUserRequest;
import com.example.shoozy_shop.dto.request.AdminUpdateUserRequest;
import com.example.shoozy_shop.dto.request.ChangePasswordRequest;
import com.example.shoozy_shop.dto.request.UpdateUserRequest;
import com.example.shoozy_shop.dto.response.UserDto;



import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;

/**
 * Các phương thức xử lý thao tác với User (profile + admin + staff).
 */
public interface UserService {
    // Profile của chính user
    UserDto getProfile(String email);

    UserDto updateProfile(String email, UpdateUserRequest request);

    // Admin methods
    Page<UserDto> searchUsers(String roleName, Boolean status, String keyword, Pageable pageable);

    UserDto changeUserStatus(Long userId, Boolean isActive);

    UserDto changeUserRole(Long userId, String roleName);

    // Mới: Staff + Admin methods

    /**
     * Lấy danh sách user theo role (ví dụ "Customer", "Staff", "Admin")
     */
    List<UserDto> getUsersByRole(String roleName);

    /**
     * Lấy chi tiết một user bất kỳ theo id
     */
    UserDto getUserById(Long userId);

    /**
     * Đổi mật khẩu cho user hiện tại (dựa vào email).
     *
     * @param email   email user đang login
     * @param request chứa oldPassword và newPassword
     * @return UserDto (optional, hoặc có thể chỉ trả HTTP 200)
     */
    UserDto changePassword(String email, ChangePasswordRequest request);

    UserDto createUser(AdminCreateUserRequest req);

    UserDto updateUser(Long userId, AdminUpdateUserRequest req);

    void deleteUser(Long userId);

    // Avatar methods
    String updateUserAvatarById(Long userId, MultipartFile avatarFile);

}
