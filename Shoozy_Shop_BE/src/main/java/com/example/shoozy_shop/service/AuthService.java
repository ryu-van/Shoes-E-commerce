package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.AuthRequest;
import com.example.shoozy_shop.dto.request.ForgotPasswordRequest;
import com.example.shoozy_shop.dto.request.RegisterRequest;
import com.example.shoozy_shop.dto.request.ResetPasswordRequest;
import com.example.shoozy_shop.dto.response.AuthResponse;

/**
 * Định nghĩa các phương thức cho chức năng Auth (đăng ký, đăng nhập, đăng xuất).
 */
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    void logout(String token);
     // -- mới --
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
