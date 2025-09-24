package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.CouponCreateRequest;
import com.example.shoozy_shop.dto.request.CouponUpdateRequest;
import com.example.shoozy_shop.dto.response.CouponForOrderResponse;
import com.example.shoozy_shop.dto.response.CouponResponse;
import com.example.shoozy_shop.exception.CouponException;
import com.example.shoozy_shop.model.Coupon;
import com.example.shoozy_shop.model.DefineStatus;
import com.example.shoozy_shop.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CouponService implements ICouponService {

    private final CouponRepository couponRepository;

    // ======= CONSTANTS (dùng chung cho create/update) =======
    private static final long MAX_ORDER_AMOUNT = 100_000_000L; // trần bảo vệ
    private static final long MIN_ORDER_MIN    = 10_000L;      // đơn tối thiểu tối thiểu
    private static final long MIN_PAYABLE_VND  = 1_000L;       // KHÔNG cho đơn về 0đ
    private static final int  MAX_PERCENT      = 30;           // trần % theo chính sách

    // ============= Helpers =============
    private String normalizeCode(String raw) {
        if (raw == null) return null;
        String code = raw.trim();
        return code.isEmpty() ? null : code.toUpperCase();
    }

    // ============= Validate: CREATE (yêu cầu code) =============
    private void checkCouponValid(CouponCreateRequest req) {

        // ===== CODE =====
        String code = normalizeCode(req.getCode());
        if (code == null) {
            throw new CouponException("Mã coupon không được để trống.");
        }
        if (!code.matches("^[A-Z0-9_-]{4,20}$")) {
            throw new CouponException("Mã coupon phải 4–20 ký tự, A–Z, 0–9, _ hoặc - (không khoảng trắng).");
        }

        // ===== TÊN =====
        if (req.getName() == null || req.getName().isBlank()) {
            throw new CouponException("Tên coupon không được để trống.");
        }

        // ===== THỜI GIAN =====
        LocalDateTime startDate = req.getStartDate();
        LocalDateTime expirationDate = req.getExpirationDate();
        if (startDate == null || expirationDate == null) {
            throw new CouponException("Ngày bắt đầu và ngày hết hạn không được để trống.");
        }
        if (!expirationDate.isAfter(startDate)) {
            throw new CouponException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (expirationDate.isBefore(LocalDateTime.now())) {
            throw new CouponException("Ngày hết hạn phải sau thời điểm hiện tại.");
        }

        // ===== SỐ LƯỢNG =====
        if (req.getQuantity() == null || req.getQuantity() < 1) {
            throw new CouponException("Số lượng coupon phải ≥ 1.");
        }

        // ===== LOẠI & GIÁ TRỊ =====
        if (req.getType() == null) {
            throw new CouponException("Loại giảm giá không được để trống.");
        }

        // ===== ĐƠN TỐI THIỂU =====
        Double orderMin = req.getCondition();
        if (orderMin == null || orderMin < MIN_ORDER_MIN) {
            throw new CouponException("Điều kiện áp dụng (đơn tối thiểu) phải ≥ " + MIN_ORDER_MIN + "đ.");
        }
        if (orderMin > MAX_ORDER_AMOUNT) {
            throw new CouponException("Điều kiện áp dụng không được vượt quá " + MAX_ORDER_AMOUNT + "đ.");
        }

        if (Boolean.TRUE.equals(req.getType())) {
            // ===== NHÁNH GIẢM THEO % =====
            Double percent = req.getValue();
            if (percent == null || percent < 1 || percent > MAX_PERCENT) {
                throw new CouponException("Giá trị phần trăm giảm phải trong khoảng 1–" + MAX_PERCENT + "%.");
            }

            Double cap = req.getValueLimit();
            if (cap == null || cap < 1_000) {
                throw new CouponException("Giới hạn số tiền giảm tối đa phải ≥ 1.000đ (bắt buộc với giảm %).");
            }
            if (cap > MAX_ORDER_AMOUNT) {
                throw new CouponException("Giới hạn số tiền giảm tối đa không được vượt quá " + MAX_ORDER_AMOUNT + "đ.");
            }

            // Tính đúng phần trăm tại ngưỡng orderMin
            long byPercentAtMin = (long) Math.floor(orderMin * percent / 100.0);
            long effectiveAtMin = Math.min(byPercentAtMin, cap.longValue());
            long maxAllow       = (long) (orderMin - MIN_PAYABLE_VND);

            // Rule: sau giảm vẫn còn ≥ MIN_PAYABLE_VND
            if (effectiveAtMin > maxAllow) {
                throw new CouponException(
                        "Cấu hình khiến đơn tối thiểu sau giảm < " + MIN_PAYABLE_VND +
                                "đ. Vui lòng giảm %, giảm 'giới hạn số tiền', hoặc tăng 'đơn tối thiểu'.");
            }

            // (Tuỳ chọn siết) cap không vượt mức % tại đơn tối thiểu
            if (cap.longValue() > byPercentAtMin) {
                throw new CouponException("Giới hạn số tiền giảm tối đa không được vượt quá " +
                        byPercentAtMin + "đ (tại mức đơn tối thiểu).");
            }

        } else {
            // ===== NHÁNH GIẢM SỐ TIỀN CỐ ĐỊNH =====
            Double valueInt = req.getValue();
            if (valueInt == null || valueInt < 1_000) {
                throw new CouponException("Số tiền giảm phải ≥ 1.000đ.");
            }
            long amount = valueInt.longValue();
            if (amount > MAX_ORDER_AMOUNT) {
                throw new CouponException("Số tiền giảm không được vượt quá " + MAX_ORDER_AMOUNT + "đ.");
            }

            // orderMin ≥ amount + minPayable
            if (orderMin < amount + MIN_PAYABLE_VND) {
                throw new CouponException("Đơn tối thiểu phải ≥ (số tiền giảm) + " + MIN_PAYABLE_VND + "đ.");
            }

            // valueLimit nếu FE gửi kèm thì không được âm
            if (req.getValueLimit() != null && req.getValueLimit() < 0) {
                throw new CouponException("Giá trị giới hạn không hợp lệ.");
            }
        }
    }

    // ============= Validate: UPDATE (KHÔNG yêu cầu code) =============
    private void checkCouponValid(CouponUpdateRequest req) {

        // ===== TÊN =====
        if (req.getName() == null || req.getName().isBlank()) {
            throw new CouponException("Tên coupon không được để trống.");
        }

        // ===== THỜI GIAN =====
        LocalDateTime startDate = req.getStartDate();
        LocalDateTime expirationDate = req.getExpirationDate();
        if (startDate == null || expirationDate == null) {
            throw new CouponException("Ngày bắt đầu và ngày hết hạn không được để trống.");
        }
        if (!expirationDate.isAfter(startDate)) {
            throw new CouponException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (expirationDate.isBefore(LocalDateTime.now())) {
            throw new CouponException("Ngày hết hạn phải sau thời điểm hiện tại.");
        }

        // ===== SỐ LƯỢNG =====
        if (req.getQuantity() == null || req.getQuantity() < 1) {
            throw new CouponException("Số lượng coupon phải ≥ 1.");
        }

        // ===== LOẠI & GIÁ TRỊ =====
        if (req.getType() == null) {
            throw new CouponException("Loại giảm giá không được để trống.");
        }

        // ===== ĐƠN TỐI THIỂU =====
        Double orderMin = req.getCondition();
        if (orderMin == null || orderMin < MIN_ORDER_MIN) {
            throw new CouponException("Điều kiện áp dụng (đơn tối thiểu) phải ≥ " + MIN_ORDER_MIN + "đ.");
        }
        if (orderMin > MAX_ORDER_AMOUNT) {
            throw new CouponException("Điều kiện áp dụng không được vượt quá " + MAX_ORDER_AMOUNT + "đ.");
        }

        if (Boolean.TRUE.equals(req.getType())) {
            // ===== NHÁNH GIẢM THEO % =====
            Double percent = req.getValue();
            if (percent == null || percent < 1 || percent > MAX_PERCENT) {
                throw new CouponException("Giá trị phần trăm giảm phải trong khoảng 1–" + MAX_PERCENT + "%.");
            }

            Double cap = req.getValueLimit();
            if (cap == null || cap < 1_000) {
                throw new CouponException("Giới hạn số tiền giảm tối đa phải ≥ 1.000đ (bắt buộc với giảm %).");
            }
            if (cap > MAX_ORDER_AMOUNT) {
                throw new CouponException("Giới hạn số tiền giảm tối đa không được vượt quá " + MAX_ORDER_AMOUNT + "đ.");
            }

            long byPercentAtMin = (long) Math.floor(orderMin * percent / 100.0);
            long effectiveAtMin = Math.min(byPercentAtMin, cap.longValue());
            long maxAllow       = (long) (orderMin - MIN_PAYABLE_VND);

            if (effectiveAtMin > maxAllow) {
                throw new CouponException(
                        "Cấu hình khiến đơn tối thiểu sau giảm < " + MIN_PAYABLE_VND +
                                "đ. Vui lòng giảm %, giảm 'giới hạn số tiền', hoặc tăng 'đơn tối thiểu'.");
            }
            if (cap.longValue() > byPercentAtMin) {
                throw new CouponException("Giới hạn số tiền giảm tối đa không được vượt quá " +
                        byPercentAtMin + "đ (tại mức đơn tối thiểu).");
            }

        } else {
            // ===== NHÁNH GIẢM SỐ TIỀN CỐ ĐỊNH =====
            Double valueInt = req.getValue();
            if (valueInt == null || valueInt < 1_000) {
                throw new CouponException("Số tiền giảm phải ≥ 1.000đ.");
            }
            long amount = valueInt.longValue();
            if (amount > MAX_ORDER_AMOUNT) {
                throw new CouponException("Số tiền giảm không được vượt quá " + MAX_ORDER_AMOUNT + "đ.");
            }
            if (orderMin < amount + MIN_PAYABLE_VND) {
                throw new CouponException("Đơn tối thiểu phải ≥ (số tiền giảm) + " + MIN_PAYABLE_VND + "đ.");
            }
            if (req.getValueLimit() != null && req.getValueLimit() < 0) {
                throw new CouponException("Giá trị giới hạn không hợp lệ.");
            }
        }
    }

    // ---------- Public APIs ----------
    @Override
    public List<CouponResponse> getAllCoupons() throws Exception {
        return couponRepository.findAll()
                .stream()
                .map(CouponResponse::convertToResponse)
                .toList();
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest couponRequest) {
        // Validate CREATE (có code)
        checkCouponValid(couponRequest);

        // Chuẩn hóa & check trùng mã
        String code = normalizeCode(couponRequest.getCode());
        if (code == null) {
            throw new CouponException("Mã coupon không được để trống.");
        }
        if (couponRepository.findByCode(code) != null) {
            throw new CouponException("Mã coupon đã tồn tại: " + code);
        }

        DefineStatus status = DefineStatus.defineStatusSmartForCoupon(
                couponRequest.getStartDate(), couponRequest.getExpirationDate(), couponRequest.getQuantity());

        Coupon coupon = Coupon.builder()
                .name(couponRequest.getName())
                .code(code)
                .description(couponRequest.getDescription())
                .startDate(couponRequest.getStartDate())
                .expirationDate(couponRequest.getExpirationDate())
                .value(couponRequest.getValue())
                .condition(couponRequest.getCondition())
                .quantity(couponRequest.getQuantity())
                .status(status.getValue())          // Integer status (enum value)
                .type(couponRequest.getType())      // true: %, false: fixed
                .valueLimit(couponRequest.getValueLimit())
                .build();

        Coupon saved = couponRepository.save(coupon);
        return CouponResponse.convertToResponse(saved);
    }

    @Override
    public CouponResponse getCouponById(Long id) throws Exception {
        return couponRepository.findById(id)
                .map(CouponResponse::convertToResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long id, CouponUpdateRequest couponRequest) throws Exception {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new Exception("Coupon not found with id: " + id));

        // ✅ Validate UPDATE (không yêu cầu code)
        checkCouponValid(couponRequest);

        // KHÔNG đụng vào coupon.code
        coupon.setName(couponRequest.getName());
        coupon.setDescription(couponRequest.getDescription());
        coupon.setStartDate(couponRequest.getStartDate());
        coupon.setExpirationDate(couponRequest.getExpirationDate());
        coupon.setValue(couponRequest.getValue());
        coupon.setCondition(couponRequest.getCondition());
        coupon.setQuantity(couponRequest.getQuantity());
        coupon.setType(couponRequest.getType());
        coupon.setValueLimit(couponRequest.getValueLimit());

        DefineStatus status = DefineStatus.defineStatusSmartForCoupon(
                couponRequest.getStartDate(), couponRequest.getExpirationDate(), couponRequest.getQuantity());
        coupon.setStatus(status.getValue());

        Coupon updated = couponRepository.save(coupon);
        return CouponResponse.convertToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) throws Exception {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new Exception("Coupon not found with id: " + id));
        coupon.setStatus(DefineStatus.DELETED.getValue());
        couponRepository.save(coupon);
    }

    @Override
    public Page<CouponResponse> getCouponsByPage(String name, LocalDate startDate, LocalDate expirationDate, Integer status, Pageable pageable) throws Exception {
        return couponRepository.getCouponPage(name, startDate, expirationDate, status, pageable);
    }

    @Override
    @Transactional
    public void updateCouponStatus(Long id) throws Exception {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new Exception("Coupon not found with id: " + id));

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(now)) {
            coupon.setStatus(DefineStatus.UPCOMING.getValue());
        } else if (
                (coupon.getStartDate() == null || !coupon.getStartDate().isAfter(now)) &&
                        (coupon.getExpirationDate() == null || !coupon.getExpirationDate().isBefore(now))
        ) {
            coupon.setStatus(DefineStatus.LAUNCHING.getValue());
        } else if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(now)) {
            coupon.setStatus(DefineStatus.EXPIRED.getValue());
        }

        couponRepository.save(coupon);
    }

    private boolean isActiveStatus(Integer status) {
        return status != null && status.equals(DefineStatus.LAUNCHING.getValue());
    }

    private boolean isDeleted(Integer status) {
        return status != null && status.equals(DefineStatus.DELETED.getValue());
    }

    private boolean isExpired(Integer status) {
        return status != null && status.equals(DefineStatus.EXPIRED.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public CouponForOrderResponse getCouponForOrder(String codeCoupon, Long userId) {
        if (codeCoupon == null || codeCoupon.isBlank()) {
            throw new CouponException("Mã giảm giá không hợp lệ.");
        }

        final String code = normalizeCode(codeCoupon);
        Coupon coupon = couponRepository.findByCode(code);
        if (coupon == null) {
            throw new CouponException("Mã giảm giá không tồn tại: " + code);
        }

        // Kiểm tra trạng thái
        if (isDeleted(coupon.getStatus())) {
            throw new CouponException("Mã giảm giá đã bị xóa.");
        }
        if (isExpired(coupon.getStatus())) {
            throw new CouponException("Mã giảm giá đã hết hạn.");
        }
        if (!isActiveStatus(coupon.getStatus())) {
            throw new CouponException("Mã giảm giá chưa đến thời gian áp dụng.");
        }

        // Bảo hiểm theo mốc thời gian (nếu status chưa đồng bộ)
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(now)) {
            throw new CouponException("Mã giảm giá chưa đến thời gian áp dụng.");
        }
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(now)) {
            throw new CouponException("Mã giảm giá đã hết hạn.");
        }

        // Check theo user
        boolean inUseOrInvalidForUser = couponRepository.checkCouponInUse(code, userId);
        if (inUseOrInvalidForUser) {
            throw new CouponException("Mã giảm giá đã được sử dụng hoặc không hợp lệ cho người dùng này.");
        }

        return CouponForOrderResponse.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .type(coupon.getType())
                .condition(coupon.getCondition())
                .value(coupon.getValue())
                .valueLimit(coupon.getValueLimit())
                .status(coupon.getStatus())
                .build();
    }

    @Override
    public List<CouponForOrderResponse> getAllCouponForOrder(Long idUser, BigDecimal moneyOrder) throws Exception {
        if (idUser == null || idUser <= 0) {
            throw new CouponException("Người dùng không hợp lệ.");
        }
        if (moneyOrder == null || moneyOrder.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponException("Số tiền đơn hàng không hợp lệ.");
        }

        List<Coupon> coupons = couponRepository.findAllByStatus(1);

        return coupons.stream()
                .filter(coupon -> {
                    // Lọc theo điều kiện đơn hàng
                    if (coupon.getCondition() != null && moneyOrder.compareTo(BigDecimal.valueOf(coupon.getCondition())) < 0) {
                        return false;
                    }
                    // Lọc theo user đã dùng chưa
                    boolean inUseOrInvalidForUser = couponRepository.checkCouponInUse(coupon.getCode(), idUser);
                    return !inUseOrInvalidForUser;
                })
                .map(coupon -> CouponForOrderResponse.builder()
                        .id(coupon.getId())
                        .name(coupon.getName())
                        .code(coupon.getCode())
                        .type(coupon.getType())
                        .condition(coupon.getCondition())
                        .value(coupon.getValue())
                        .valueLimit(coupon.getValueLimit())
                        .status(coupon.getStatus())
                        .build()
                )
                .toList();
    }
}
