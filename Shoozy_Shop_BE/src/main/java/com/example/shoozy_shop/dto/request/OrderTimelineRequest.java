package com.example.shoozy_shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class OrderTimelineRequest {

    @NotNull(message = "Order ID cannot be null")
    private Long orderId;

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotBlank(message = "Type cannot be blank")
    private String type;

    @NotBlank(message = "Description cannot be blank")
    private String description;
}
