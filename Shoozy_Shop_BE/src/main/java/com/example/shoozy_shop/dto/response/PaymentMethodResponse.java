package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.model.PaymentMethod;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {
    private Long id;
    private String name;
    private String type;

public static PaymentMethodResponse fromModel(PaymentMethod paymentMethod) {
        return PaymentMethodResponse.builder()
                .id(paymentMethod.getId())
                .name(paymentMethod.getName())
                .type(paymentMethod.getType())
                .build();
    }
}
