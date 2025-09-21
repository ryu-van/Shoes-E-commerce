package com.example.shoozy_shop.config;

import com.example.shoozy_shop.service.OrderService;
import com.example.shoozy_shop.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventsListener {

    private final WebSocketService webSocketService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderService.OrderCreatedEvent e) {
        webSocketService.broadcastRefresh(
                "order",
                Map.of("orderId", e.getOrderId(), "orderCode", e.getOrderCode()),
                "ORDER_CREATED"
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCouponDecremented(OrderService.CouponDecrementedEvent e) {
        // giảm lượt & status cho mọi client
        webSocketService.broadcastCouponUpdate(
                e.getCouponId(), e.getCode(), e.getQuantity(), e.getStatus(), "DECREMENT"
        );
        // thông báo admin: đơn hàng có coupon
        webSocketService.broadcastOrderWithCoupon(
                e.getOrderId(), e.getOrderCode(), e.getCode(), e.getQuantity(), "COUPON_USED"
        );
        // (tuỳ chọn) kênh chỉ báo số lượng
        webSocketService.broadcastCouponQuantityUpdate(
                e.getCouponId(), e.getCode(), e.getQuantity(), e.getStatus()
        );
    }
}