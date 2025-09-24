// com/example/shoozy_shop/dto/request/UpdateReturnStatusRequest.java
package com.example.shoozy_shop.dto.request;

import com.example.shoozy_shop.enums.RefundMethod;
import com.example.shoozy_shop.enums.ReturnStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateReturnStatusRequest {

    @NotNull
    private Long returnRequestId;

    @NotNull
    private ReturnStatus status; // PENDING/APPROVED/...

    // ⬇️ Chỉ dùng khi status == REFUNDED (validate điều kiện sẽ check ở Service –
    // bước 4)
    private RefundMethod refundMethod; // CASH / BANK_TRANSFER / EWALLET
    private String referenceCode; // mã giao dịch hoặc để trống với CASH (sẽ auto-sinh ở bước 4)
    private String refundNote; // ghi chú hoàn tiền (tuỳ chọn)
}
