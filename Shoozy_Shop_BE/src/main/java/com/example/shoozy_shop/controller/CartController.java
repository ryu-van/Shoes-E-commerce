package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.AddToCartRequest;
import com.example.shoozy_shop.dto.request.ChangeQuantityRequest;
import com.example.shoozy_shop.dto.response.ApiResponse;
import com.example.shoozy_shop.dto.response.ProductCartResponse;
import com.example.shoozy_shop.model.CartItem;
import com.example.shoozy_shop.service.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/carts")
public class CartController {

    private final ICartService cartService;

    // Thêm sản phẩm vào giỏ hàng
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody AddToCartRequest req) throws Exception {
        CartItem cartItem = cartService.addToCart(req.getUserId(), req.getProductVariantId(), req.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Product added to cart successfully", cartItem));
    }

    // Xóa cart item theo cartId và cartItemId
//    @DeleteMapping("/{cartId}/items/{cartItemId}")
//    public ResponseEntity<?> deleteCartItem(@PathVariable Long cartId,
//                                            @PathVariable Long cartItemId) throws Exception {
//        cartService.deleteItemCart(cartItemId, cartId);
//        return ResponseEntity.ok(ApiResponse.success("Cart item deleted successfully", null));
//    }

    // Đổi số lượng cart item theo cartItemId
    @PutMapping("/items/{cartItemId}/quantity")
    public ResponseEntity<?> changeCartItemQuantity(@PathVariable Long cartItemId,
                                                    @RequestBody ChangeQuantityRequest req) throws Exception {
        cartService.changeQuantity(cartItemId, req.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Cart item quantity updated successfully", null));
    }

    // Lấy toàn bộ sản phẩm trong giỏ hàng của user (trả về List<ProductCartResponse>)
    @GetMapping("/user/{userId}/items")
    public ResponseEntity<?> getCartItems(@PathVariable Long userId) {
        List<ProductCartResponse> items = cartService.getProductCartResponses(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart items fetched successfully", items));
    }

    @DeleteMapping("/delete/{cartItemId}")
    public ResponseEntity<?> deleteCartItem(@PathVariable Long cartItemId) throws Exception {
        cartService.deleteCartItem(cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Cart item deleted successfully", null));
    }

}
