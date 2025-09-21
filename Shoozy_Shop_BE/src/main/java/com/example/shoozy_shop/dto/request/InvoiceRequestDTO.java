package com.example.shoozy_shop.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequestDTO {
    private Long userId; // ID người dùng đặt hàng
    private Long paymentMethodId; // ID phương thức thanh toán
    private Long couponId; // ID mã giảm giá (nếu có)
    private String fullname; // Tên khách hàng
    private String phoneNumber; // Số điện thoại
    private String email;
    private String shippingAddress; // Địa chỉ giao hàng
    private String note; // Ghi chú
    private LocalDate orderDate; // Ngày đặt hàng
    private String shippingMethod; // Phương thức vận chuyển
    private LocalDate shippingDate; // Ngày giao dự kiến
    private String status;
    private List<OrderItemRequestDTO> items; // Danh sách sản phẩm
}
