package com.example.shoozy_shop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketService implements IWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastRefresh(String type, Object data, String action) {
        Map<String, Object> message = Map.of(
                "type", type,
                "timestamp", LocalDateTime.now(),
                "payload", data,
                "action", action
        );
        messagingTemplate.convertAndSend("/topic/refresh", message);
        log.info("Broadcasted refresh message of type: {}", type);
    }

    /**
     * Gửi thông báo cập nhật coupon cụ thể cho admin
     * Đảm bảo format message đồng nhất cho frontend
     */
    public void broadcastCouponUpdate(Long couponId, String code, Integer quantity, Integer status, String action) {
        // Message format cho /topic/refresh (tương thích với code hiện tại)
        Map<String, Object> refreshMessage = Map.of(
                "type", "coupon",
                "action", action,
                "timestamp", LocalDateTime.now(),
                "payload", Map.of(
                        "couponId", couponId,
                        "code", code,
                        "quantity", quantity,
                        "status", status
                )
        );

        // Message format mới cho các channel riêng biệt
        Map<String, Object> couponUpdateMessage = Map.of(
                "type", "COUPON_UPDATE",
                "couponId", couponId,
                "code", code,
                "quantity", quantity,
                "status", status,
                "action", action,
                "timestamp", LocalDateTime.now()
        );

        // Gửi đến topic chung (format cũ để tương thích)
        messagingTemplate.convertAndSend("/topic/refresh", refreshMessage);

        // Gửi đến các topic riêng (format mới)
        messagingTemplate.convertAndSend("/topic/coupon/status", couponUpdateMessage);
        messagingTemplate.convertAndSend("/topic/admin/coupon", couponUpdateMessage);

        log.info("Broadcasted coupon update: {} - {} - Quantity: {} - Action: {}", code, couponId, quantity, action);
    }

    /**
     * Gửi thông báo cho admin khi có đơn hàng mới sử dụng coupon
     */
    public void broadcastOrderWithCoupon(Long orderId, String orderCode, String couponCode, Integer couponQuantity, String action) {
        Map<String, Object> orderUpdate = Map.of(
                "type", "ORDER_WITH_COUPON",
                "orderId", orderId,
                "orderCode", orderCode,
                "couponCode", couponCode,
                "couponQuantity", couponQuantity,
                "action", action,
                "timestamp", LocalDateTime.now()
        );

        // Gửi đến topic admin orders
        messagingTemplate.convertAndSend("/topic/admin/orders", orderUpdate);

        // Cũng gửi đến topic refresh để các component khác có thể lắng nghe
        messagingTemplate.convertAndSend("/topic/refresh", orderUpdate);

        log.info("Broadcasted order with coupon: Order {} - Coupon {} - Remaining: {} - Action: {}",
                orderCode, couponCode, couponQuantity, action);
    }

    /**
     * Gửi thông báo trạng thái kết nối cho admin
     */
    public void sendAdminStatus(String sessionId, String status, String message) {
        Map<String, Object> statusUpdate = Map.of(
                "type", "ADMIN_STATUS",
                "sessionId", sessionId,
                "status", status,
                "message", message,
                "timestamp", LocalDateTime.now()
        );

        messagingTemplate.convertAndSendToUser(sessionId, "/topic/admin/status", statusUpdate);
        log.info("Sent admin status to session {}: {} - {}", sessionId, status, message);
    }

    /**
     * Gửi thông báo cập nhật số lượng coupon real-time cho tất cả user đang xem
     */
    public void broadcastCouponQuantityUpdate(Long couponId, String code, Integer quantity, Integer status) {
        Map<String, Object> quantityUpdate = Map.of(
                "type", "COUPON_QUANTITY_UPDATE",
                "couponId", couponId,
                "code", code,
                "quantity", quantity,
                "status", status,
                "timestamp", LocalDateTime.now()
        );

        // Gửi đến topic public để tất cả user có thể thấy
        messagingTemplate.convertAndSend("/topic/coupon/quantity", quantityUpdate);

        log.info("Broadcasted coupon quantity update: {} - Quantity: {}", code, quantity);
    }

    /**
     * Gửi thông báo coupon hết hàng
     */
    public void broadcastCouponOutOfStock(Long couponId, String code) {
        Map<String, Object> outOfStockMessage = Map.of(
                "type", "COUPON_OUT_OF_STOCK",
                "couponId", couponId,
                "code", code,
                "timestamp", LocalDateTime.now()
        );

        // Gửi đến tất cả các channel liên quan
        messagingTemplate.convertAndSend("/topic/coupon/status", outOfStockMessage);
        messagingTemplate.convertAndSend("/topic/admin/coupon", outOfStockMessage);
        messagingTemplate.convertAndSend("/topic/refresh", outOfStockMessage);

        log.info("Broadcasted coupon out of stock: {}", code);
    }
}