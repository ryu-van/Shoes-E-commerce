package com.example.shoozy_shop.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class PaymentResponse implements Serializable {
    private String status;
    private String message;
    private String url;
}
