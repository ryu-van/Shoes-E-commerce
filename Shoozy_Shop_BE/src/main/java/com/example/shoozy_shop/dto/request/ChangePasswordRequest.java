package com.example.shoozy_shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Old password must not be blank")
    private String oldPassword;

    @NotBlank(message = "New password must not be blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$",
            message = "New password must be at least 6 characters long, contain uppercase, lowercase, and a number"
    )
    private String newPassword;

    @NotBlank(message = "Confirm password must not be blank")
    @Size(min = 6, message = "Confirm password must be at least 6 characters long")
    private String confirmPassword;
}
