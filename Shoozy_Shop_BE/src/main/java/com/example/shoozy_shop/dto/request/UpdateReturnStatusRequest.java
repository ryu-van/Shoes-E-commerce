package com.example.shoozy_shop.dto.request;

import com.example.shoozy_shop.enums.ReturnStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateReturnStatusRequest {
    @NotNull(message = "ID yêu cầu trả hàng không được để trống")
    private Long returnRequestId;

    @NotNull(message = "Trạng thái mới không được để trống")
    private ReturnStatus status;
}
