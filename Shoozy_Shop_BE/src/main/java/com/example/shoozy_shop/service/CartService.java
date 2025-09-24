package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.response.ProductCartResponse;
import com.example.shoozy_shop.exception.OutOfStockException;
import com.example.shoozy_shop.model.Cart;
import com.example.shoozy_shop.model.CartItem;
import com.example.shoozy_shop.model.ProductVariant;
import com.example.shoozy_shop.model.User;
import com.example.shoozy_shop.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CartService implements ICartService {

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public CartItem addToCart(Long userId, Long productVariantId, Integer quantity) throws Exception {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));
            cart = new Cart();
            cart.setUser(user);
            cart = cartRepository.save(cart);
        }

        ProductVariant productVariant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new Exception("Product variant not found"));

        final int stock = productVariant.getQuantity();

        CartItem existingCartItem = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), productVariantId);
        int cartQty = (existingCartItem != null) ? existingCartItem.getQuantity() : 0;
        int allowAdd = Math.max(stock - cartQty, 0);
        if (quantity > allowAdd) {
            throw new OutOfStockException(cartQty, allowAdd);
        }
        if (existingCartItem != null) {
            existingCartItem.setQuantity(cartQty + quantity);
            return cartItemRepository.save(existingCartItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProductVariant(productVariant);
            cartItem.setQuantity(quantity);
            return cartItemRepository.save(cartItem);
        }
    }

//    @Override
//    @Transactional
//    public void deleteItemCart(Long cartItemId, Long cartId) throws Exception {
//        CartItem cartItem = cartItemRepository.findByIdAndCartId(cartItemId, cartId)
//                .orElseThrow(() -> new Exception("Cart item not found in this cart"));
//        cartItemRepository.delete(cartItem);
//    }

    @Override
    @Transactional
    public void changeQuantity(Long cartItemId, Integer quantity) throws Exception {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new Exception("Cart item not found"));
        ProductVariant productVariant = cartItem.getProductVariant();
        if (productVariant == null) {
            throw new Exception("Product variant not found for this cart item");
        }
        final int stock = productVariant.getQuantity();
        int cartQty = cartItem.getQuantity();
        int allowAdd = Math.max(stock - cartQty, 0);
        if (quantity > stock) {
            throw new OutOfStockException(cartQty,allowAdd);
        }
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }


    @Override
    public List<ProductCartResponse> getProductCartResponses(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) return Collections.emptyList();
        return cartItemRepository.findCartItemsByCartId(cart.getId());
    }

    @Override
    public void deleteCartItem(Long cartItemId) throws Exception {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new Exception("Cart item not found"));
        cartItemRepository.delete(cartItem);
    }


}
