package com.example.shoozy_shop.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    @NotNull(message = "userId không được để trống")
    private Long userId;
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private Long paymentMethodId;
    @NotBlank(message = "Họ tên không được để trống")
    private String fullname;
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    private String note;

    private LocalDateTime shippingDate;

    private String couponCode;

    private Boolean type;

    private BigDecimal shippingFee;

    private List<Long> cartItemId;

    private List<OrderDetailRequest> orderDetails;
}
