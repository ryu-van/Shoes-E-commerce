package com.example.shoozy_shop.ScheduledTasks;

import com.example.shoozy_shop.model.Coupon;
import com.example.shoozy_shop.model.DefineStatus;
import com.example.shoozy_shop.model.Promotion;
import com.example.shoozy_shop.repository.CouponRepository;
import com.example.shoozy_shop.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoUpdateStatus {
    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;

    @Scheduled(fixedRate = 600000) // chạy mỗi 6p
    public void updatePromotionStatus() {
        List<Promotion> promotions = promotionRepository.findAll();
        int updatedCount = 0;

        for (Promotion promotion : promotions) {
            int newStatus = DefineStatus.defineStatus(
                    promotion.getStartDate(), promotion.getExpirationDate()
            ).getValue();

            if (promotion.getStatus() != newStatus) {
                promotion.setStatus(newStatus);
                promotionRepository.save(promotion);
                updatedCount++;
                log.info("Đã cập nhật Promotion ID={} sang trạng thái={}",
                        promotion.getId(), newStatus);
            }
        }

        if (updatedCount > 0) {
            log.info("✅ Hoàn tất cập nhật, tổng số promotion thay đổi: {}", updatedCount);
        } else {
            log.info("⏳ Không có promotion nào cần cập nhật trong lần chạy này.");
        }
    }
    @Scheduled(fixedRate = 5000)
    public void updateCouponStatus() {
        // Chỉ lấy những coupon có thể cần update
        List<Coupon> coupons = couponRepository.findCouponsNeedingStatusUpdate();

        if (coupons.isEmpty()) {
            log.debug("⏳ Không có coupon nào cần cập nhật trong lần chạy này.");
            return;
        }

        List<Coupon> couponsToUpdate = new ArrayList<>();

        for (Coupon coupon : coupons) {
            int currentStatus = coupon.getStatus();
            int newStatus;

            // Ưu tiên check quantity trước
            if (coupon.getQuantity() == 0) {
                newStatus = DefineStatus.EXHAUSTED.getValue();
            } else {
                newStatus = DefineStatus.defineStatus(
                        coupon.getStartDate(),
                        coupon.getExpirationDate()
                ).getValue();
            }

            if (currentStatus != newStatus) {
                coupon.setStatus(newStatus);
                couponsToUpdate.add(coupon);
                log.info("Đã chuẩn bị cập nhật Coupon ID={} từ trạng thái {} sang {}",
                        coupon.getId(), currentStatus, newStatus);
            }
        }

        if (!couponsToUpdate.isEmpty()) {
            couponRepository.saveAll(couponsToUpdate);
            log.info("✅ Hoàn tất cập nhật, tổng số coupon thay đổi: {}", couponsToUpdate.size());
        }
    }
}
