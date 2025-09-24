package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.CouponCreateRequest;
import com.example.shoozy_shop.dto.request.CouponSendRequest;
import com.example.shoozy_shop.dto.request.CouponUpdateRequest;
import com.example.shoozy_shop.dto.response.CouponForOrderResponse;
import com.example.shoozy_shop.dto.response.CouponResponse;
import com.example.shoozy_shop.dto.response.ListCouponResponse;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.service.EmailService;
import com.example.shoozy_shop.service.ICouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ICouponService couponService;
    private final EmailService emailService;

    // Lấy danh sách tất cả coupon
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAllCoupons() throws Exception {
        List<CouponResponse> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(ApiResponse.success("Get all coupons successfully", coupons));
    }

    // Tạo mới coupon
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@RequestBody CouponCreateRequest couponRequest) throws Exception {
        CouponResponse createdCoupon = couponService.createCoupon(couponRequest);
        return ResponseEntity.ok(ApiResponse.success("Create coupon successfully", createdCoupon));
    }

    // Cập nhật coupon theo ID
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable("id") Long id,
            @RequestBody CouponUpdateRequest couponRequest) throws Exception {
        CouponResponse updatedCoupon = couponService.updateCoupon(id, couponRequest);
        return ResponseEntity.ok(ApiResponse.success("Update coupon successfully", updatedCoupon));
    }

    // Xóa coupon theo ID
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<?>> deleteCoupon(@PathVariable("id") Long id) throws Exception {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Delete coupon successfully", null));
    }

    // Cập nhật trạng thái coupon (true/false)
    @PutMapping("/update-status/{id}")
    public ResponseEntity<ApiResponse<?>> updateCouponStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") Boolean status) throws Exception {
        couponService.updateCouponStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Update coupon status successfully", null));
    }

    // Lọc & phân trang coupon
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<?>> getCouponsByPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "expirationDate", required = false) LocalDate expirationDate,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) throws Exception {

        Pageable pageable = PageRequest.of(page, limit);
        Page<CouponResponse> couponResponses = couponService.getCouponsByPage(keyword, startDate, expirationDate, status, pageable);
        List<CouponResponse> couponList = couponResponses.getContent();
        ListCouponResponse listCouponResponse = ListCouponResponse.builder()
                .coupons(couponList)
                .totalPage(couponResponses.getTotalPages())
                .totalElements((int) couponResponses.getTotalElements())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Get coupons by page successfully", listCouponResponse));
    }

    // Lấy coupon theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable("id") Long id) throws Exception {
        CouponResponse coupon = couponService.getCouponById(id);
        return ResponseEntity.ok(ApiResponse.success("Get coupon by ID successfully", coupon));
    }

    // Lấy coupon theo mã code dùng cho đơn hàng
    @GetMapping("/order")
    public ResponseEntity<ApiResponse<CouponForOrderResponse>> getCouponForOrder(@RequestParam("code") String codeCoupon, @RequestParam("id_user") Long idUser) throws Exception {
        CouponForOrderResponse coupon = couponService.getCouponForOrder(codeCoupon, idUser);
        return ResponseEntity.ok(ApiResponse.success("Get coupon for order successfully", coupon));
    }

    @GetMapping("/order/all")
    public ResponseEntity<ApiResponse<List<CouponForOrderResponse>>> getAllCouponForOrder(@RequestParam("id_user") Long idUser, @RequestParam("money_order") BigDecimal moneyOrder) throws Exception {
        List<CouponForOrderResponse> coupons = couponService.getAllCouponForOrder(idUser, moneyOrder);
        return ResponseEntity.ok(ApiResponse.success("Get all coupon for order successfully", coupons));
    }

    @PostMapping("/send-coupon")
    public ResponseEntity<ApiResponse<?>> sendCouponToCustomer(@RequestBody CouponSendRequest req) throws Exception {
        emailService.sendCouponToCustomer(req);
        return ResponseEntity.ok(ApiResponse.success("Send coupon to customer successfully", null));
    }

}
