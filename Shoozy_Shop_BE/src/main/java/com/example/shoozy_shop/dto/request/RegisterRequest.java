package com.example.shoozy_shop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Full name must not be empty")
    private String fullname;

    @NotBlank(message = "Email must not be empty")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password must not be empty")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$",
            message = "Password must be at least 6 characters long, contain uppercase, lowercase, and a number"
    )
    private String password;

    @NotBlank(message = "Confirm password must not be empty")
    @Size(min = 6, message = "Confirm password must be at least 6 characters long")
    private String confirmPassword;

    @NotBlank(message = "Phone number must not be empty")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Phone number must start with 0 and have exactly 10 digits"
    )
    private String phoneNumber;
}
