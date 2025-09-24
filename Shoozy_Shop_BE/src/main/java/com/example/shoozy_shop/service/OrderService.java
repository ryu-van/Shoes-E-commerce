package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.*;
import com.example.shoozy_shop.dto.response.OrderResponse;
import com.example.shoozy_shop.dto.response.OrderStatusStatisticsDto;
import com.example.shoozy_shop.exception.CouponException;
import com.example.shoozy_shop.exception.ForbiddenException;
import com.example.shoozy_shop.exception.OutOfStockException;
import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.*;
import com.example.shoozy_shop.repository.*;
import com.example.shoozy_shop.dto.response.OrderDetailResponse;
import com.example.shoozy_shop.dto.response.TransactionResponse;
import com.example.shoozy_shop.enums.RefundStatus;
import com.example.shoozy_shop.enums.ReturnStatus;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {

    @lombok.Value
    public static class OrderCreatedEvent {
        Long orderId;
        String orderCode;
    }

    @lombok.Value
    public static class CouponDecrementedEvent {
        Long couponId;
        String code;
        Integer quantity;
        Integer status;
        Long orderId;
        String orderCode;
    }


    private final ReturnItemRepository returnItemRepository;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CouponRepository couponRepository;
    private final TransactionRepository transactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderTimelineRepository orderTimelineRepository;
    private final OrderTimelineService orderTimelineService;
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionRepository promotionRepository;
    private final ReviewRepository reviewRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketService webSocketService;
    private final EventQueue eventQueue;

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional
    public Order addOrder(OrderRequest orderRequest) {
        User existingUser = userRepository.findById(orderRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("user", orderRequest.getUserId()));

        PaymentMethod existingPaymentMethod = paymentMethodRepository.findById(orderRequest.getPaymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("payment method", orderRequest.getPaymentMethodId()));

        Coupon coupon = (orderRequest.getCouponCode() != null && !orderRequest.getCouponCode().isEmpty())
                ? couponRepository.findByCode(orderRequest.getCouponCode())
                : null;

        Order newOrder = Order.builder()
                .orderCode(generateUniqueOrderCode())
                .user(existingUser)
                .paymentMethod(existingPaymentMethod)
                .fullname(orderRequest.getFullname())
                .phoneNumber(orderRequest.getPhoneNumber())
                .address(orderRequest.getAddress())
                .note(orderRequest.getNote())
                .active(true)
                .type(orderRequest.getType() != null ? orderRequest.getType() : false)
                .status("PENDING")
                .build();

        Order savedOrder = orderRepository.save(newOrder);

        BigDecimal totalBeforeDiscount = ZERO;

        // Lặp qua từng item
        for (OrderDetailRequest detailReq : orderRequest.getOrderDetails()) {
            ProductVariant pv = productVariantRepository.findByIdForUpdate(detailReq.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("product variant", detailReq.getProductVariantId()));

            int reqQuantity = detailReq.getQuantity();
            int available = pv.getQuantity();
            if (available <= 0 || reqQuantity > available) {
                int allowAdd = Math.max(0, available);
                throw new OutOfStockException(reqQuantity, allowAdd);
            }

            pv.setQuantity(available - reqQuantity);
            productVariantRepository.save(pv);

            java.math.BigDecimal originalPrice = java.math.BigDecimal.valueOf(pv.getSellPrice()); // đơn giá 1 sp
            BigDecimal promotionDiscountAmount = ZERO;

            Promotion promotion = null;
            if (detailReq.getIdPromotion() != null) {
                promotion = promotionRepository.findById(detailReq.getIdPromotion()).orElse(null);
                ProductPromotion pp = promotionProductRepository
                        .findByPromotionIdAndProductVariantId(detailReq.getIdPromotion(), detailReq.getProductVariantId());

                if (pp != null) {
                    Double discountPercent = pp.getCustomValue();
                    if (discountPercent == null && pp.getPromotion() != null) {
                        discountPercent = pp.getPromotion().getValue();
                    }
                    if (discountPercent != null) {
                        promotionDiscountAmount = originalPrice
                                .multiply(BigDecimal.valueOf(discountPercent))
                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    }
                }
            }

            BigDecimal finalPricePerItem = originalPrice.subtract(promotionDiscountAmount);
            BigDecimal lineTotal = finalPricePerItem.multiply(BigDecimal.valueOf(reqQuantity));

            OrderDetail od = new OrderDetail();
            od.setOrder(savedOrder);
            od.setProductVariant(pv);
            od.setPrice(originalPrice.doubleValue());
            od.setQuantity(reqQuantity);
            od.setTotalMoney(originalPrice.multiply(BigDecimal.valueOf(reqQuantity)).doubleValue());
            od.setFinalPrice(lineTotal);

            if (promotion != null) {
                od.setPromotionCode(promotion.getCode());
                od.setPromotionName(promotion.getName());
                od.setPromotionValue(promotion.getValue());
                od.setPromotionDiscountAmount(promotionDiscountAmount.doubleValue());
            }

            orderDetailRepository.save(od);
            totalBeforeDiscount = totalBeforeDiscount.add(lineTotal);
        }

        // Áp mã giảm giá
        BigDecimal couponDiscountAmount = ZERO;
        Coupon fresh = null;

        if (coupon != null) {
            savedOrder.setCoupon(coupon);
            savedOrder.setCouponCode(coupon.getCode());
            savedOrder.setCouponName(coupon.getName());
            savedOrder.setCouponType(coupon.getType());
            if (coupon.getValue() != null) {
                savedOrder.setCouponValue(BigDecimal.valueOf(coupon.getValue()));
            }

            BigDecimal couponVal = BigDecimal.valueOf(coupon.getValue() == null ? 0.0 : coupon.getValue());
            if (Boolean.TRUE.equals(coupon.getType())) {
                BigDecimal couponLimit = (coupon.getValueLimit() == null)
                        ? null
                        : BigDecimal.valueOf(coupon.getValueLimit());
                couponDiscountAmount = totalBeforeDiscount
                        .multiply(couponVal)
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (couponLimit != null && couponDiscountAmount.compareTo(couponLimit) > 0) {
                    couponDiscountAmount = couponLimit;
                }
                if (coupon.getValueLimit() != null) {
                    savedOrder.setCouponValueLimit(BigDecimal.valueOf(coupon.getValueLimit()));
                }
            } else {
                couponDiscountAmount = couponVal.min(totalBeforeDiscount);
            }

            int changed = couponRepository.decrementIfAvailable(coupon.getId());
            if (changed == 0) {
                throw new CouponException("Mã giảm giá đã hết lượt.");
            }

            fresh = couponRepository.findById(coupon.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("coupon", coupon.getId()));

            Integer left = fresh.getQuantity();
            if (left != null && left == 0) {
                couponRepository.updateStatus(fresh.getId(), DefineStatus.EXHAUSTED.getValue());
                fresh.setStatus(DefineStatus.EXHAUSTED.getValue());
            }
        }

        // Tính phí ship và tổng cuối
        BigDecimal shippingFee = orderRequest.getShippingFee();
        savedOrder.setShippingFee(shippingFee);
        if (shippingFee == null || shippingFee.signum() < 0) {
            shippingFee = ZERO;
        }

        BigDecimal netMerchandise = totalBeforeDiscount.subtract(couponDiscountAmount);
        if (netMerchandise.signum() < 0) {
            netMerchandise = ZERO;
        }

        BigDecimal finalPrice = netMerchandise.add(shippingFee);

        savedOrder.setCouponDiscountAmount(couponDiscountAmount);
        savedOrder.setTotalMoney(totalBeforeDiscount);
        savedOrder.setFinalPrice(finalPrice);

        savedOrder = orderRepository.save(savedOrder);

        if (orderRequest.getCartItemId() != null && !orderRequest.getCartItemId().isEmpty()) {
            for (Long cartItemId : orderRequest.getCartItemId()) {
                cartItemRepository.deleteById(cartItemId);
            }
        }

        // Đăng ký đẩy sự kiện vào queue SAU commit
        final Long orderIdForEvent = savedOrder.getId();
        final String orderCodeForEvent = savedOrder.getOrderCode();
        final Coupon freshFinal = fresh;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventQueue.enqueue(new OrderCreatedEvent(orderIdForEvent, orderCodeForEvent));
                if (coupon != null && freshFinal != null) {
                    eventQueue.enqueue(new CouponDecrementedEvent(
                            freshFinal.getId(),
                            freshFinal.getCode(),
                            freshFinal.getQuantity(),
                            freshFinal.getStatus(),
                            orderIdForEvent,
                            orderCodeForEvent
                    ));
                }
            }
        });

        // Lấy user hiện tại từ Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("user", email));
        String role = currentUser.getRole().getName();
        OrderTimeline cancelTimeline = new OrderTimeline();
        cancelTimeline.setOrder(savedOrder);
        cancelTimeline.setUser(currentUser);
        cancelTimeline.setType("ORDER_CREATED");
        cancelTimeline.setDescription("Đơn hàng đã được tạo bởi " + role.toLowerCase() + ".");
        cancelTimeline.setCreateDate(LocalDateTime.now());
        return savedOrder;
    }


    private String generateUniqueOrderCode() {
        String code;
        do {
            code = generateInvoiceCode();
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    private String generateInvoiceCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")); // ví dụ: 250716
        int random = new Random().nextInt(1_000); // 000 → 999
        return String.format("HD%s%03d", datePart, random); // ví dụ: HD250716042
    }

    @Override
    @Transactional
    public Order updateOrder(Long id, OrderRequest orderRequest) {
        // 1. Tìm đơn hàng
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order", id));

        // 2. Cập nhật user và payment method nếu có
        User exUser = userRepository.findById(orderRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("user", orderRequest.getUserId()));

        PaymentMethod exPaymentMethod = paymentMethodRepository.findById(orderRequest.getPaymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("payment method", orderRequest.getPaymentMethodId()));

        existingOrder.setUser(exUser);
        existingOrder.setPaymentMethod(exPaymentMethod);
        existingOrder.setNote(orderRequest.getNote());
        existingOrder.setFullname(orderRequest.getFullname());
        existingOrder.setPhoneNumber(orderRequest.getPhoneNumber());
        existingOrder.setAddress(orderRequest.getAddress());

        // 3. Tính lại tổng tiền (nếu cần)
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(id);

        // 4. Gửi thông báo
        webSocketService.broadcastRefresh("order", existingOrder, "ORDER_UPDATED");

        return orderRepository.save(existingOrder);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.getOrderWithDetails(id);
        if (order == null) {
            throw new ResourceNotFoundException("order", id);
        }
        // Lấy transactions riêng để tránh duplicate
        List<Transaction> transactions = transactionRepository.findByOrderId(id);
        order.setTransactions(transactions);

        // Build orderDetails có review
        List<OrderDetailResponse> orderDetails = order.getOrderDetails() != null
                ? order.getOrderDetails().stream()
                .map(od -> {
                    int refundedQty = returnItemRepository
                            .sumReturnedQuantityByOrderDetailIdAndStatus(od.getId(), ReturnStatus.REFUNDED);

                    return OrderDetailResponse.fromEntity(od, order.getUser().getId(), reviewRepository,
                            refundedQty);
                })
                .toList()
                : null;

        return getOrderResponse(order, orderDetails);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order", id));

        // Lấy user hiện tại từ Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("user", email));

        String role = currentUser.getRole().getName();
        String status = existingOrder.getStatus();

        // Kiểm tra quyền hủy đơn
        if (role.equals("CUSTOMER")) {
            // CUSTOMER chỉ hủy được khi PENDING
            if (!status.equals("PENDING")) {
                throw new IllegalStateException("Khách hàng chỉ có thể hủy đơn khi đang chờ xác nhận.");
            }
        } else if (role.equals("ADMIN") || role.equals("STAFF")) {
            // ADMIN/STAFF hủy được khi PENDING, CONFIRMED, PROCESSING
            if (status.equals("SHIPPING") || status.equals("DELIVERED") || status.equals("COMPLETED")) {
                throw new IllegalStateException("Không thể hủy đơn ở trạng thái hiện tại.");
            }
        }

        // Thay đổi trạng thái đơn hàng
        existingOrder.setActive(false);
        existingOrder.setStatus("CANCELLED");
        orderRepository.save(existingOrder);

        // Gửi WebSocket thông báo
        webSocketService.broadcastRefresh("order", existingOrder, "ORDER_CANCELLED");

        // Rollback lại số lượng sản phẩm và coupon
        this.retoreStockAndCoupon(existingOrder);

        // Hủy giao dịch pending
        List<Transaction> transactions = transactionRepository.findByOrderId(id);
        for (Transaction transaction : transactions) {
            if (transaction.getStatus().equals("PENDING")) {
                transaction.setStatus("CANCELLED");
                transactionRepository.save(transaction);
            }
        }

        // Lưu timeline
        OrderTimeline cancelTimeline = new OrderTimeline();
        cancelTimeline.setOrder(existingOrder);
        cancelTimeline.setUser(currentUser);
        cancelTimeline.setType("ORDER_CANCELLED");
        cancelTimeline.setDescription("Đơn hàng đã bị hủy bởi " + role.toLowerCase() + ".");
        cancelTimeline.setCreateDate(LocalDateTime.now());
        orderTimelineRepository.save(cancelTimeline);
    }

    @Override
    @Transactional
    public Order updateStatus(Long id, StatusOrderRequest statusOrderRequest) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order", id));

        List<Transaction> transactions = transactionRepository.findByOrderId(id);
        existingOrder.setStatus(statusOrderRequest.getStatus());

        if ("DELIVERED".equalsIgnoreCase(statusOrderRequest.getStatus())) {
            // CHỈ COD (ONLINE + CASH) mới auto thu hộ thành công
            boolean isCOD = Boolean.TRUE.equals(existingOrder.getType())
                    && existingOrder.getPaymentMethod() != null
                    && Long.valueOf(1L).equals(existingOrder.getPaymentMethod().getId());
            if (isCOD) {
                for (Transaction t : transactions) {
                    if ("PENDING".equalsIgnoreCase(t.getStatus())) {
                        t.setStatus("SUCCESS");
                        t.setCompletedDate(LocalDateTime.now());
                    }
                }
                transactionRepository.saveAll(transactions);
            }
        }

        webSocketService.broadcastRefresh("order", existingOrder, "ORDER_UPDATED_STATUS");
        return orderRepository.save(existingOrder);
    }


    @Override
    public Order updateInfo(Long id, UpdateUserOrderInfoRequest updateUserOrderInfoRequest) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order", id));
        existingOrder.setFullname(updateUserOrderInfoRequest.getFullName());
        existingOrder.setPhoneNumber(updateUserOrderInfoRequest.getPhoneNumber());
        existingOrder.setAddress(updateUserOrderInfoRequest.getAddress());
        existingOrder.setNote(updateUserOrderInfoRequest.getNote());
        webSocketService.broadcastRefresh("order", existingOrder, "ORDER_UPDATED_INFO");
        return orderRepository.save(existingOrder);
    }

    @Override
    public List<OrderStatusStatisticsDto> getOrderStatusStatistics(String filterType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (filterType) {
            case "7days" -> startDate = now.minusDays(7);
            case "month" -> startDate = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
            case "year" -> startDate = now.withDayOfYear(1).toLocalDate().atStartOfDay();
            default -> throw new IllegalArgumentException("Invalid filter type: " + filterType);
        }

        List<OrderStatusStatisticsDto> stats = orderRepository.getOrderStatusStatisticsBetween(startDate, now);

        long total = stats.stream().mapToLong(OrderStatusStatisticsDto::getTotalOrders).sum();

        for (OrderStatusStatisticsDto dto : stats) {
            double percent = total > 0 ? (dto.getTotalOrders() * 100.0) / total : 0.0;
            dto.setPercentage(Math.round(percent * 100.0) / 100.0); // làm tròn 2 số
        }

        return stats;
    }

    @Override
    public List<OrderResponse> getAllOrderByUserId(Long userId) {
        List<Order> orders = orderRepository.getAllOrderByUserId(userId);
        return orders.stream()
                .map(order -> {
                    List<OrderDetailResponse> orderDetails = order.getOrderDetails() != null
                            ? order.getOrderDetails().stream()
                            .map(od -> OrderDetailResponse.fromEntity(od, order.getUser().getId(),
                                    reviewRepository))
                            .toList()
                            : null;
                    return getOrderResponse(order, orderDetails);
                })
                .toList();
    }

    private OrderResponse getOrderResponse(Order order, List<OrderDetailResponse> orderDetails) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .type(order.getType())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .fullName(order.getFullname())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .status(order.getStatus())
                .totalMoney(order.getTotalMoney())
                .shippingFee(order.getShippingFee())
                .finalPrice(order.getFinalPrice())
                .couponDiscountAmount(order.getCouponDiscountAmount())
                .active(order.getActive())
                .user(order.getUser())
                .paymentMethod(order.getPaymentMethod())
                .coupon(order.getCoupon())
                .note(order.getNote())
                .orderDetails(orderDetails)
                .transactions(order.getTransactions() != null ? order.getTransactions().stream()
                        .map(TransactionResponse::fromEntity)
                        .toList() : null)
                .build();
    }

    @Override
    public List<OrderDetail> getReturnableItems(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Đơn hàng không thuộc về bạn");
        }

        List<OrderDetail> allDetails = new ArrayList<>(order.getOrderDetails());

        // Lấy tất cả return items của orderId
        Map<Long, Integer> returnedMap = returnItemRepository.findAllByOrderId(orderId)
                .stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.getOrderDetail().getId(), // Lấy id từ OrderDetail
                        Collectors.summingInt(ReturnItem::getQuantity)));

        return allDetails.stream()
                .filter(od -> {
                    int returnedQty = returnedMap.getOrDefault(od.getId(), 0);
                    return returnedQty < od.getQuantity(); // chỉ hiển thị nếu chưa trả hết
                })
                .collect(Collectors.toList());
    }

    private void retoreStockAndCoupon(Order order) {
        for (OrderDetail detail : order.getOrderDetails()) {
            ProductVariant pv = detail.getProductVariant();
            pv.setQuantity(pv.getQuantity() + detail.getQuantity());
            productVariantRepository.save(pv);
        }
        if (order.getCoupon() != null) {
            Coupon coupon = order.getCoupon();
            coupon.setQuantity(coupon.getQuantity() + 1);
            couponRepository.save(coupon);

            // Đẩy event sau commit để cập nhật realtime
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventQueue.enqueue(new CouponDecrementedEvent(
                            coupon.getId(),
                            coupon.getCode(),
                            coupon.getQuantity(),
                            coupon.getStatus(),
                            order.getId(),
                            order.getOrderCode()
                    ));
                }
            });
        }
    }

}