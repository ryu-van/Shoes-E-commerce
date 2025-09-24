package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.response.ApiResponse;
import com.example.shoozy_shop.service.OrderEmailService;
import com.example.shoozy_shop.service.OrderService;
import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.model.User;
import com.example.shoozy_shop.repository.OrderRepository;
import com.example.shoozy_shop.repository.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/order-email")
@RequiredArgsConstructor
@Slf4j
public class OrderEmailController {

    private final OrderEmailService orderEmailService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * API gửi email thông báo đặt hàng thành công
     * POST /api/v1/order-email/send-success/{orderId}
     */
    @PostMapping("/send-success/{orderId}")
    @PreAuthorize("hasAuthority('Admin') or hasAuthority('Customer')")
    public ResponseEntity<ApiResponse<String>> sendOrderSuccessEmail(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
            
            User user = userRepository.findById(order.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));

            orderEmailService.sendOrderSuccessEmail(order, user);
            
            log.info("Order success email sent for order ID: {}", orderId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Email thông báo đặt hàng thành công đã được gửi!",
                    "Order success email sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending order success email for order ID: {}", orderId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Lỗi khi gửi email: " + e.getMessage()
            ));
        }
    }

    /**
     * API gửi email thông báo đơn hàng bị hủy bởi người dùng
     * POST /api/v1/order-email/send-cancelled-by-user/{orderId}
     */
    @PostMapping("/send-cancelled-by-user/{orderId}")
    @PreAuthorize("hasAuthority('Admin') or hasAuthority('Customer')")
    public ResponseEntity<ApiResponse<String>> sendOrderCancelledByUserEmail(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
            
            User user = userRepository.findById(order.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));

            orderEmailService.sendOrderCancelledByUserEmail(order, user);
            
            log.info("Order cancelled by user email sent for order ID: {}", orderId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Email thông báo hủy đơn hàng đã được gửi!",
                    "Order cancelled by user email sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending order cancelled by user email for order ID: {}", orderId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Lỗi khi gửi email: " + e.getMessage()
            ));
        }
    }

    /**
     * API gửi email thông báo đơn hàng hoàn thành
     * POST /api/v1/order-email/send-completed/{orderId}
     */
    @PostMapping("/send-completed/{orderId}")
    @PreAuthorize("hasAuthority('Admin') or hasAuthority('Staff') or hasAuthority('Customer')")
    public ResponseEntity<ApiResponse<String>> sendOrderCompletedEmail(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
            
            User user = userRepository.findById(order.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));

            orderEmailService.sendOrderCompletedEmail(order, user);
            
            log.info("Order completed email sent for order ID: {}", orderId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Email thông báo hoàn thành đơn hàng đã được gửi!",
                    "Order completed email sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending order completed email for order ID: {}", orderId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Lỗi khi gửi email: " + e.getMessage()
            ));
        }
    }

    /**
     * API gửi email thông báo đơn hàng bị hủy bởi shop
     * POST /api/v1/order-email/send-cancelled-by-shop/{orderId}
     */
    @PostMapping("/send-cancelled-by-shop/{orderId}")
    @PreAuthorize("hasAuthority('Admin') or hasAuthority('Staff')")
    public ResponseEntity<ApiResponse<String>> sendOrderCancelledByShopEmail(
            @PathVariable Long orderId,
            @RequestBody(required = false) CancelReasonRequest cancelReason) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
            
            User user = userRepository.findById(order.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));

            String reason = cancelReason != null ? cancelReason.getReason() : null;
            orderEmailService.sendOrderCancelledByShopEmail(order, user, reason);
            
            log.info("Order cancelled by shop email sent for order ID: {}", orderId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Email thông báo shop hủy đơn hàng đã được gửi!",
                    "Order cancelled by shop email sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending order cancelled by shop email for order ID: {}", orderId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Lỗi khi gửi email: " + e.getMessage()
            ));
        }
    }

    /**
     * API gửi email thông báo đặt hàng thành công cho email cụ thể
     * POST /api/v1/order-email/send-success-to-email
     */
    @PostMapping("/send-success-to-email")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<ApiResponse<String>> sendOrderSuccessEmailToSpecificEmail(
            @RequestBody SendEmailRequest request) {
        try {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + request.getOrderId()));
            
            User user = userRepository.findById(order.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));

            // Tạo user tạm với email được chỉ định
            User tempUser = new User();
            tempUser.setFullname(user.getFullname());
            tempUser.setEmail(request.getEmail());

            orderEmailService.sendOrderSuccessEmail(order, tempUser);
            
            log.info("Order success email sent to {} for order ID: {}", request.getEmail(), request.getOrderId());
            return ResponseEntity.ok(ApiResponse.success(
                    "Email thông báo đặt hàng thành công đã được gửi đến " + request.getEmail() + "!",
                    "Order success email sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending order success email to {} for order ID: {}", request.getEmail(), request.getOrderId(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Lỗi khi gửi email: " + e.getMessage()
            ));
        }
    }

    /**
     * API test gửi email đơn giản (không cần order thật)
     * POST /api/v1/order-email/test-simple
     */
    @PostMapping("/test-simple")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<ApiResponse<String>> testSimpleEmail(@RequestBody TestSimpleEmailRequest request) {
        try {
            // Tạo order và user giả để test
            Order mockOrder = new Order();
            mockOrder.setId(999L);
            mockOrder.setTotalMoney(java.math.BigDecimal.valueOf(500000.0)); // Sử dụng BigDecimal
            mockOrder.setCreatedAt(java.time.LocalDateTime.now());
            mockOrder.setOrderDetails(java.util.Collections.emptySet()); // Tránh lỗi null pointer

            User mockUser = new User();
            mockUser.setFullname("Nguyễn Văn Test");
            mockUser.setEmail(request.getEmail());

            switch (request.getEmailType()) {
                case "success":
                    orderEmailService.sendOrderSuccessEmail(mockOrder, mockUser);
                    break;
                case "cancelled_by_user":
                    orderEmailService.sendOrderCancelledByUserEmail(mockOrder, mockUser);
                    break;
                case "cancelled_by_shop":
                    orderEmailService.sendOrderCancelledByShopEmail(mockOrder, mockUser, "Test reason");
                    break;
                case "completed":
                    orderEmailService.sendOrderCompletedEmail(mockOrder, mockUser);
                    break;
                default:
                    return ResponseEntity.badRequest().body(ApiResponse.error("Loại email không hợp lệ"));
            }
            
            log.info("Test email sent to {} with type: {}", request.getEmail(), request.getEmailType());
            return ResponseEntity.ok(ApiResponse.success(
                    "Email test đã được gửi thành công!",
                    "Test email sent successfully"
            ));
        } catch (Exception e) {
            log.error("Error sending test email to {} with type: {}", request.getEmail(), request.getEmailType(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Lỗi khi gửi email test: " + e.getMessage()
            ));
        }
    }

    // DTO classes
    @Data
    public static class CancelReasonRequest {
        private String reason;
    }

    @Data
    public static class SendEmailRequest {
        @NotNull(message = "Order ID không được để trống")
        private Long orderId;
        
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        private String email;
    }

    @Data
    public static class TestEmailRequest {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        private String email;
        
        @NotBlank(message = "Loại email không được để trống")
        private String emailType; // success, cancelled_by_user, cancelled_by_shop
    }

    @Data
    public static class TestSimpleEmailRequest {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        private String email;
        
        @NotBlank(message = "Loại email không được để trống")
        private String emailType; // success, cancelled_by_user, cancelled_by_shop
    }
}
