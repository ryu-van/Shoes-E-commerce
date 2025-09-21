package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.response.ProductCartResponse;
import com.example.shoozy_shop.model.Cart;
import com.example.shoozy_shop.model.CartItem;
import com.example.shoozy_shop.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public interface ICartService  {
    CartItem addToCart(Long userId, Long productVariantId, Integer quantity) throws Exception;
    void deleteItemCart(Long cartItemId,Long cartId) throws Exception;
    void changeQuantity(Long cartItemId, Integer quantity) throws Exception;
    List<ProductCartResponse> getProductCartResponses(Long userId);
    void deleteCartItem(Long cartItemId) throws Exception;
}
