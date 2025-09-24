package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.CreateReturnRequestRequest;
import com.example.shoozy_shop.dto.request.ReturnItemRequest;
import com.example.shoozy_shop.dto.response.OrderSummaryDto;
import com.example.shoozy_shop.dto.response.ReturnItemResponse;
import com.example.shoozy_shop.dto.response.ReturnRequestResponse;
import com.example.shoozy_shop.enums.RefundMethod;
import com.example.shoozy_shop.enums.RefundStatus;
import com.example.shoozy_shop.enums.ReturnStatus;
import com.example.shoozy_shop.exception.CustomException;
import com.example.shoozy_shop.exception.ForbiddenException;
import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.*;
import com.example.shoozy_shop.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnServiceImpl implements ReturnService {
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final OrderRepository orderRepository;
    @Autowired
    private final OrderDetailRepository orderDetailRepository;
    @Autowired
    private final ReturnRequestRepository returnRequestRepository;
    @Autowired
    private final ReturnItemRepository returnItemRepository;
    @Autowired
    private final ReturnItemImageRepository returnItemImageRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private RefundTransactionRepository refundTransactionRepository;
    @Autowired
    private RefundInfoRepository refundInfoRepository;
    @Autowired
    private final MinioService minioService;

    @Override
    public List<ReturnRequestResponse> getReturnsByUserId(Long userId, String q, String status) {
        ReturnStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = ReturnStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CustomException("Trạng thái không hợp lệ: " + status, HttpStatus.BAD_REQUEST.value());
            }
        }

        String keyword = (q != null && !q.isBlank()) ? q.trim() : null;

        List<ReturnRequest> found = returnRequestRepository.searchByUserNoPage(userId, keyword, statusEnum);
        return found.stream().map(this::convertToDto).toList();
    }

    @Override
    public ReturnRequest getReturnRequestByIdAndUser(Long id, Long userId) {
        ReturnRequest request = returnRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu trả hàng"));

        if (!request.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Yêu cầu trả hàng không thuộc về bạn");
        }

        return request;
    }

    @Override
    public Page<ReturnRequestResponse> getAllForAdmin(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ReturnRequest> requests;
        if (status != null && !status.isBlank()) {
            ReturnStatus enumStatus;
            try {
                enumStatus = ReturnStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CustomException("Trạng thái không hợp lệ: " + status, HttpStatus.BAD_REQUEST.value());
            }

            requests = returnRequestRepository.findByStatus(enumStatus, pageable);
        } else {
            requests = returnRequestRepository.findAll(pageable);
        }

        return requests.map(this::convertToDto);
    }

    @Override
    public ReturnRequestResponse getById(Long returnRequestId) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu trả hàng"));
        return convertToDto(request);
    }

    private BigDecimal safeBD(Double d) {
        return d == null ? BigDecimal.ZERO : BigDecimal.valueOf(d);
    }

    /** Đơn giá sau KM theo sản phẩm (CHƯA phân bổ coupon cấp đơn) */
    private BigDecimal unitAfterLinePromotion(OrderDetail od) {
        int qty = od.getQuantity() == null ? 0 : od.getQuantity();
        BigDecimal lineAfterPromo = od.getFinalPrice() == null ? BigDecimal.ZERO : od.getFinalPrice(); // TỔNG dòng
        if (qty <= 0)
            return BigDecimal.ZERO;
        return lineAfterPromo.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
    }

    /**
     * Đơn giá CUỐI để hoàn tiền:
     * = (lineAfterPromo - allocatedCouponForLine) / qty
     * - lineAfterPromo = od.finalPrice (TỔNG dòng sau KM theo sản phẩm)
     * - Phân bổ coupon cấp đơn theo tỷ trọng lineAfterPromo / sumAllLinesAfterPromo
     */
    private BigDecimal unitFinalForRefund(Order order, OrderDetail od) {
        int qty = od.getQuantity() == null ? 0 : od.getQuantity();
        if (qty <= 0)
            return BigDecimal.ZERO;

        BigDecimal lineAfterPromo = od.getFinalPrice() == null ? BigDecimal.ZERO : od.getFinalPrice();
        BigDecimal coupon = order.getCouponDiscountAmount() == null ? BigDecimal.ZERO : order.getCouponDiscountAmount();

        BigDecimal sumLinesAfterPromo = order.getOrderDetails().stream()
                .map(d -> d.getFinalPrice() == null ? BigDecimal.ZERO : d.getFinalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal allocated = BigDecimal.ZERO;
        if (coupon.signum() > 0 && sumLinesAfterPromo.signum() > 0) {
            allocated = coupon.multiply(lineAfterPromo)
                    .divide(sumLinesAfterPromo, 2, RoundingMode.HALF_UP);
        }

        BigDecimal netLine = lineAfterPromo.subtract(allocated);
        return netLine.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    public ReturnRequest createReturnRequest(CreateReturnRequestRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Đơn hàng không thuộc về bạn");
        }
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể trả hàng nếu đơn hàng đã hoàn thành");
        }
        LocalDateTime completedAt = order.getUpdatedAt(); // lý tưởng là dùng deliveredAt nếu có
        if (completedAt == null || completedAt.plusDays(7).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Đơn hàng đã quá thời gian cho phép trả hàng (7 ngày)");
        }

        // chặn trùng orderDetail trong 1 request
        var ids = request.getItems().stream().map(ReturnItemRequest::getOrderDetailId).toList();
        if (ids.size() != ids.stream().distinct().count()) {
            throw new CustomException("Danh sách sản phẩm trả có mục trùng lặp", HttpStatus.BAD_REQUEST.value());
        }

        ReturnRequest rr = new ReturnRequest();
        rr.setUser(user);
        rr.setOrder(order);
        rr.setReason(request.getReason());
        rr.setNote(request.getNote());
        rr.setStatus(ReturnStatus.PENDING);
        rr.setCreatedAt(LocalDateTime.now());
        rr.setUpdatedAt(LocalDateTime.now());
        rr = returnRequestRepository.save(rr);
        var infoReq = request.getRefundInfo();
        if (infoReq != null && infoReq.getMethod() != null) {
            // validate theo method
            switch (infoReq.getMethod()) {
                case BANK_TRANSFER -> {
                    if (isBlank(infoReq.getBankName()) || isBlank(infoReq.getAccountNumber())
                            || isBlank(infoReq.getAccountHolder())) {
                        throw new CustomException("Thiếu thông tin ngân hàng để hoàn tiền", 400);
                    }
                }
                case EWALLET -> {
                    if (isBlank(infoReq.getWalletProvider()) || isBlank(infoReq.getWalletAccount())) {
                        throw new CustomException("Thiếu thông tin ví điện tử để hoàn tiền", 400);
                    }
                }
                case CASH -> {
                    /* không cần gì thêm */ }
            }

            RefundInfo info = new RefundInfo();
            info.setReturnRequest(rr);
            info.setMethod(infoReq.getMethod());
            info.setBankName(infoReq.getBankName());
            info.setAccountNumber(infoReq.getAccountNumber());
            info.setAccountHolder(infoReq.getAccountHolder());
            info.setWalletProvider(infoReq.getWalletProvider());
            info.setWalletAccount(infoReq.getWalletAccount());
            refundInfoRepository.save(info);
        }

        List<ReturnItem> createdItems = new ArrayList<>();
        BigDecimal totalRefund = BigDecimal.ZERO;

        for (ReturnItemRequest itemReq : request.getItems()) {
            OrderDetail od = orderDetailRepository.findById(itemReq.getOrderDetailId())
                    .orElseThrow(() -> new RuntimeException("Chi tiết đơn hàng không tồn tại"));

            // bảo đảm chi tiết thuộc đơn này
            if (!od.getOrder().getId().equals(order.getId())) {
                throw new CustomException("Sản phẩm không thuộc đơn hàng này", HttpStatus.BAD_REQUEST.value());
            }

            int qty = itemReq.getQuantity();
            if (qty <= 0)
                throw new CustomException("Số lượng phải > 0", HttpStatus.BAD_REQUEST.value());

            // tính số lượng còn có thể trả = đã mua - đã yêu cầu trả (các request còn hiệu
            // lực)
            Integer alreadyRequested = returnItemRepository
                    .sumRequestedQtyActiveByOrderDetailId(od.getId());
            int purchased = od.getQuantity() == null ? 0 : od.getQuantity();
            int remaining = purchased - (alreadyRequested == null ? 0 : alreadyRequested);
            if (qty > remaining) {
                throw new CustomException("Số lượng trả vượt quá số còn lại có thể trả",
                        HttpStatus.BAD_REQUEST.value());
            }

            // đơn giá cuối để hoàn = đã trừ promotion + phân bổ coupon
            BigDecimal unitFinal = unitFinalForRefund(order, od);
            totalRefund = totalRefund.add(unitFinal.multiply(BigDecimal.valueOf(qty)));

            // lưu ReturnItem
            ReturnItem ri = new ReturnItem();
            ri.setReturnRequest(rr);
            ri.setOrderDetail(od);
            ri.setQuantity(qty);
            ri.setNote(itemReq.getNote());
            ri = returnItemRepository.save(ri);

            // lưu ảnh nếu có
            if (itemReq.getImageUrls() != null && !itemReq.getImageUrls().isEmpty()) {
                for (String url : itemReq.getImageUrls()) {
                    ReturnItemImage img = new ReturnItemImage();
                    img.setReturnItem(ri);
                    img.setImageUrl(url);
                    img.setCreatedAt(LocalDateTime.now());
                    returnItemImageRepository.save(img);
                }
                ri.setImages(returnItemImageRepository.findByReturnItemId(ri.getId()));
            }

            createdItems.add(ri);
        }

        rr.setReturnItems(createdItems);
        totalRefund = totalRefund.setScale(0, RoundingMode.HALF_UP);
        rr.setRefundAmount(totalRefund); // ✅ chỉ tiền hàng, không cộng ship
        return returnRequestRepository.save(rr);
    }

    @Override
    public ReturnRequestResponse convertToDto(ReturnRequest returnRequest) {
        ReturnRequestResponse response = new ReturnRequestResponse();
        response.setId(returnRequest.getId());
        response.setReason(returnRequest.getReason());
        response.setNote(returnRequest.getNote());
        response.setStatus(returnRequest.getStatus().name());
        response.setRefundAmount(returnRequest.getRefundAmount());
        response.setCreatedAt(returnRequest.getCreatedAt());
        response.setUpdatedAt(returnRequest.getUpdatedAt());
        refundTransactionRepository.findByReturnRequestId(returnRequest.getId()).ifPresent(tx -> {
            var txDto = new com.example.shoozy_shop.dto.response.RefundTransactionDto();
            txDto.setAmount(tx.getAmount());
            txDto.setMethod(tx.getMethod().name());
            txDto.setReferenceCode(tx.getReferenceCode());
            txDto.setNote(tx.getNote());
            txDto.setCreatedBy(tx.getCreatedBy());
            txDto.setCreatedAt(tx.getCreatedAt());
            response.setRefundTransaction(txDto);
        });
        refundInfoRepository.findByReturnRequestId(returnRequest.getId()).ifPresent(info -> {
            var dto = new com.example.shoozy_shop.dto.response.RefundInfoDto();
            dto.setMethod(info.getMethod().name());
            dto.setBankName(info.getBankName());
            dto.setAccountNumber(info.getAccountNumber());
            dto.setAccountHolder(info.getAccountHolder());
            dto.setWalletProvider(info.getWalletProvider());
            dto.setWalletAccount(info.getWalletAccount());
            response.setRefundInfo(dto);
        });

        // Convert đơn hàng
        Order order = returnRequest.getOrder();
        OrderSummaryDto orderDto = new OrderSummaryDto();
        orderDto.setId(order.getId());
        orderDto.setOrderCode(order.getOrderCode());
        orderDto.setFullname(order.getFullname());
        orderDto.setPhoneNumber(order.getPhoneNumber());
        orderDto.setShippingAddress(order.getAddress());
        orderDto.setTotalMoney(order.getTotalMoney());
        response.setOrder(orderDto);

        // Convert danh sách mục trả hàng
        List<ReturnItemResponse> itemDtos = returnRequest.getReturnItems().stream().map(item -> {
            ReturnItemResponse dto = new ReturnItemResponse();
            dto.setId(item.getId());
            dto.setOrderDetailId(item.getOrderDetail().getId());
            dto.setQuantity(item.getQuantity());
            dto.setNote(item.getNote());
            dto.setImageUrls(item.getImageUrls()); // đảm bảo getImageUrls hoạt động đúng
            dto.setProductName(item.getOrderDetail().getProductVariant().getProduct().getName());

            return dto;
        }).toList();
        response.setReturnItems(itemDtos);

        return response;
    }

    @Override
    @Transactional
    public void updateStatus(Long returnRequestId, String statusStr) {
        ReturnStatus newStatus = ReturnStatus.valueOf(statusStr.toUpperCase());
        updateStatus(returnRequestId, newStatus, null, null, null);
    }

    @Override
    @Transactional
    public void updateStatus(Long returnRequestId, ReturnStatus newStatus,
            RefundMethod method, String referenceCode, String refundNote) {
        ReturnRequest req = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu trả hàng"));

        if (!req.getStatus().canTransitionTo(newStatus)) {
            throw new CustomException("Chuyển trạng thái không hợp lệ", 400);
        }

        switch (newStatus) {
            case RETURNED -> {
                for (ReturnItem item : req.getReturnItems()) {
                    ProductVariant variant = item.getOrderDetail().getProductVariant();
                    variant.setQuantity(variant.getQuantity() + item.getQuantity());
                    productVariantRepository.save(variant);
                }
            }
            case REFUNDED -> {
                if (refundTransactionRepository.existsByReturnRequestId(req.getId())) {
                    throw new CustomException("Yêu cầu này đã được hoàn tiền trước đó", 400);
                }
                if (method == null) {
                    throw new CustomException("Thiếu phương thức hoàn tiền", 400);
                }
                if (method == RefundMethod.CASH && (referenceCode == null || referenceCode.isBlank())) {
                    referenceCode = generateCashRefundCode();
                }
                RefundTransaction tx = new RefundTransaction();
                tx.setReturnRequest(req);
                tx.setAmount(req.getRefundAmount());
                tx.setMethod(method);
                tx.setReferenceCode(referenceCode);
                tx.setNote(refundNote);
                tx.setCreatedBy("admin");
                // TODO: lấy từ SecurityContext
                refundTransactionRepository.save(tx);

                for (ReturnItem item : req.getReturnItems()) {
                    OrderDetail od = item.getOrderDetail();
                    od.setRefundStatus(RefundStatus.REFUNDED);
                    orderDetailRepository.save(od);
                }
            }
            case COMPLETED -> {
                /* chỉ đóng yêu cầu */ }
            default -> {
                /* các trạng thái còn lại chỉ set status */ }
        }

        req.setStatus(newStatus);
        req.setUpdatedAt(LocalDateTime.now());
        returnRequestRepository.save(req);
    }

    private String generateCashRefundCode() {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        int rnd = new java.util.Random().nextInt(9000) + 1000;
        return "CASH-" + date + "-" + rnd;
    }

}
