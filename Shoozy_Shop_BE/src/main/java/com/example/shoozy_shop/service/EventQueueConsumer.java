package com.example.shoozy_shop.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventQueueConsumer {

    private final EventQueue eventQueue;
    private final WebSocketService webSocketService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "event-queue-consumer");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void start() {
        executor.submit(() -> {
            log.info("EventQueueConsumer started");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Object evt = eventQueue.take();

                    if (evt instanceof OrderService.OrderCreatedEvent e) {
                        // Gửi về kênh tổng (đúng format FE đang nghe)
                        webSocketService.broadcastRefresh("order",
                                Map.of("id", e.getOrderId(), "orderCode", e.getOrderCode()),
                                "ORDER_CREATED");
                    } else if (evt instanceof OrderService.CouponDecrementedEvent e) {
                        // Cập nhật realtime cho Admin + mọi user
                        webSocketService.broadcastCouponUpdate(
                                e.getCouponId(), e.getCode(), e.getQuantity(), e.getStatus(), "DECREMENT");
                        webSocketService.broadcastOrderWithCoupon(
                                e.getOrderId(), e.getOrderCode(), e.getCode(), e.getQuantity(), "COUPON_USED");
                    } else {
                        log.debug("Unknown event type: {}", evt);
                    }

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    log.error("EventQueueConsumer error", ex);
                }
            }
            log.info("EventQueueConsumer stopped");
        });
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }
}