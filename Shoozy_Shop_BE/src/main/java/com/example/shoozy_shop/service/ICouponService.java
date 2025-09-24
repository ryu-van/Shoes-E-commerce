package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.CouponCreateRequest;
import com.example.shoozy_shop.dto.request.CouponUpdateRequest;
import com.example.shoozy_shop.dto.response.CouponForOrderResponse;
import com.example.shoozy_shop.dto.response.CouponResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ICouponService {

    List<CouponResponse> getAllCoupons() throws Exception;
    CouponResponse createCoupon(CouponCreateRequest couponRequest) throws Exception;
    CouponResponse getCouponById(Long id) throws Exception;
    CouponResponse updateCoupon(Long id, CouponUpdateRequest couponRequest) throws Exception;
    void deleteCoupon(Long id) throws Exception;
    Page<CouponResponse> getCouponsByPage(String name, LocalDate startDate, LocalDate expirationDate, Integer status , Pageable pageable) throws Exception;
    void updateCouponStatus(Long id) throws Exception;
    CouponForOrderResponse getCouponForOrder(String codeCoupon,Long idUser) throws Exception;
    List<CouponForOrderResponse> getAllCouponForOrder(Long idUser, BigDecimal moneyOrder) throws Exception;
}
