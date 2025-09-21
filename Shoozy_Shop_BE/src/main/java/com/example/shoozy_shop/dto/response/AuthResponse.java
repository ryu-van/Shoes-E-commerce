package com.example.shoozy_shop.dto.response;

/**
 * DTO dùng để trả token về client sau khi đăng ký hoặc đăng nhập thành công.
 */
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String role;

    public AuthResponse(String accessToken, String role) {
        this.accessToken = accessToken;
        this.role = role;
    }

    // ===== Getter / Setter =====
    public String getAccessToken() {
        return accessToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
