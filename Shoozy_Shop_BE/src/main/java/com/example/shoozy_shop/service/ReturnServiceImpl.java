package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.CreateReturnRequestRequest;
import com.example.shoozy_shop.dto.request.ReturnItemRequest;
import com.example.shoozy_shop.dto.response.OrderSummaryDto;
import com.example.shoozy_shop.dto.response.ReturnItemResponse;
import com.example.shoozy_shop.dto.response.ReturnRequestResponse;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        LocalDateTime completedAt = order.getUpdatedAt();
        if (completedAt == null || completedAt.plusDays(7).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Đơn hàng đã quá thời gian cho phép trả hàng (7 ngày)");
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setUser(user);
        returnRequest.setOrder(order);
        returnRequest.setReason(request.getReason());
        returnRequest.setNote(request.getNote());
        returnRequest.setStatus(ReturnStatus.PENDING);
        returnRequest.setCreatedAt(LocalDateTime.now());
        returnRequest.setUpdatedAt(LocalDateTime.now());
        returnRequest = returnRequestRepository.save(returnRequest);

        List<ReturnItem> createdItems = new ArrayList<>();

        for (ReturnItemRequest itemRequest : request.getItems()) {
            OrderDetail orderDetail = orderDetailRepository.findById(itemRequest.getOrderDetailId())
                    .orElseThrow(() -> new RuntimeException("Chi tiết đơn hàng không tồn tại"));

            ReturnItem returnItem = new ReturnItem();
            returnItem.setReturnRequest(returnRequest);
            returnItem.setOrderDetail(orderDetail);
            returnItem.setQuantity(itemRequest.getQuantity());
            returnItem.setReturnStatus("WAITING");
            returnItem.setNote(itemRequest.getNote());
            returnItem = returnItemRepository.save(returnItem);

            // 🖼️ Lưu ảnh từ danh sách URL đã được upload trước
            if (itemRequest.getImageUrls() != null) {
                for (String imageUrl : itemRequest.getImageUrls()) {
                    ReturnItemImage image = new ReturnItemImage();
                    image.setReturnItem(returnItem);
                    image.setImageUrl(imageUrl);
                    image.setCreatedAt(LocalDateTime.now());
                    returnItemImageRepository.save(image);
                }

                // Load lại danh sách ảnh
                returnItem.setImages(returnItemImageRepository.findByReturnItemId(returnItem.getId()));
            }

            createdItems.add(returnItem);
        }

        returnRequest.setReturnItems(createdItems);
        return returnRequest;
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
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu trả hàng"));

        // Parse status mới
        ReturnStatus newStatus;
        try {
            newStatus = ReturnStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new CustomException("Trạng thái không hợp lệ", HttpStatus.BAD_REQUEST.value());
        }

        // Check trạng thái hiện tại
        ReturnStatus currentStatus = request.getStatus();
        if (currentStatus == ReturnStatus.COMPLETED) {
            throw new CustomException("Yêu cầu đã hoàn thành, không thể cập nhật", HttpStatus.BAD_REQUEST.value());
        }

        if (newStatus == ReturnStatus.REFUNDED) {
            for (ReturnItem item : request.getReturnItems()) {
                ProductVariant variant = item.getOrderDetail().getProductVariant();
                if (variant == null) {
                    throw new CustomException("Không tìm thấy biến thể sản phẩm",
                            HttpStatus.NOT_FOUND.value());
                }

                // Cập nhật tồn kho
                variant.setQuantity(variant.getQuantity() + item.getQuantity());
                productVariantRepository.save(variant);

                // Cập nhật trạng thái hoàn tiền trong OrderDetail
                OrderDetail orderDetail = item.getOrderDetail();
                orderDetail.setRefundStatus(RefundStatus.REFUNDED);
                orderDetailRepository.save(orderDetail);
            }
        }

        // Cập nhật trạng thái yêu cầu trả hàng
        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now());
        returnRequestRepository.save(request);
    }

}
