package com.example.shoozy_shop.service;

import com.example.shoozy_shop.model.PaymentMethod;

import java.util.List;

public interface IPaymentMethodService {
    List<PaymentMethod> getAllPaymentMethods();
}
