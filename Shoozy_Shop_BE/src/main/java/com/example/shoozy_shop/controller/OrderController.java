package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.OrderRequest;
import com.example.shoozy_shop.dto.request.StatusOrderRequest;
import com.example.shoozy_shop.dto.request.UpdateUserOrderInfoRequest;
import com.example.shoozy_shop.dto.response.OrderResponse;
import com.example.shoozy_shop.dto.response.ProductCheckoutResponse;
import com.example.shoozy_shop.dto.response.ReturnableItemDto;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.model.OrderDetail;
import com.example.shoozy_shop.model.Transaction;
import com.example.shoozy_shop.repository.CartItemRepository;
import com.example.shoozy_shop.repository.ReturnRequestRepository;
import com.example.shoozy_shop.service.OrderService;
import com.example.shoozy_shop.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartItemRepository cartItemRepository;
    private final TransactionService transactionService;
    private final ReturnRequestRepository  returnRepository;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", orders));
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrdersByUserId(@RequestParam("userId") Long userId) {
        List<OrderResponse> orders = orderService.getAllOrderByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy đơn hàng của người dùng thành công", orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công", orderService.getOrderById(id)));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<Order>> addOrder(@RequestBody OrderRequest orderRequest) throws Exception {
        Order order = orderService.addOrder(orderRequest);
        Transaction transaction = transactionService.ensureInitialCashTransaction(order);
        return ResponseEntity.ok(ApiResponse.success("Tạo đơn hàng thành công", order));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> updateOrder(@PathVariable Long id,
            @RequestBody OrderRequest orderRequest) {
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật đơn hàng thành công", orderService.updateOrder(id, orderRequest)));
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<ApiResponse<Order>> updateStatus(@PathVariable Long id,
            @RequestBody StatusOrderRequest statusOrderRequest) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công",
                orderService.updateStatus(id, statusOrderRequest)));
    }

    @PutMapping("/user-info/{id}")
    public ResponseEntity<ApiResponse<Order>> updateInfo(@PathVariable Long id,
            @RequestBody UpdateUserOrderInfoRequest updateUserOrderInfoRequest) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin người mua thành công",
                orderService.updateInfo(id, updateUserOrderInfoRequest)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteOrder(@PathVariable("id") Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đơn hàng thành công", null));
    }

    @GetMapping("/checkout-items")
    public ResponseEntity<ApiResponse<?>> getCheckoutItems(@RequestParam List<Long> ids) {
        List<ProductCheckoutResponse> items = cartItemRepository.findProductCheckoutByCartItemId(ids);
        return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm trong giỏ hàng thành công", items));
    }

    @GetMapping("/{orderId}/returnable-items")
    public ResponseEntity<ApiResponse<?>> getReturnableItems(
            @PathVariable Long orderId,
            @RequestParam Long userId) {

        List<OrderDetail> returnableItems = orderService.getReturnableItems(orderId, userId);

        List<ReturnableItemDto> result = returnableItems.stream()
                .map(od -> {
                    int returnedQuantity = returnRepository.sumReturnedQuantityByOrderDetailId(od.getId()); // tổng đã
                                                                                                            // trả
                    int availableQuantity = od.getQuantity() - returnedQuantity; // còn lại
                    return new ReturnableItemDto(
                            od.getId(),
                            availableQuantity,
                            od.getProductVariant().getThumbnail(),
                            od.getProductVariant().getColor().getName(),
                            od.getProductVariant().getSize().getValue(),
                            od.getProductVariant().getProduct().getName());
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm có thể trả thành công", result));
    }

    @GetMapping("/transaction")
    public ResponseEntity<?> getTransactionByOrderCode(@RequestParam String orderCode) {
        try {
            Transaction tx = transactionService.getTransactionByCode(orderCode);
            if (tx == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Transaction not found"));
            }
            return ResponseEntity.ok(ApiResponse.success("OK", tx));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }


}
