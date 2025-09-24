package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.ProductPromotionRequest;
import com.example.shoozy_shop.dto.request.PromotionRequest;
import com.example.shoozy_shop.dto.response.PromotionDetailResponse;
import com.example.shoozy_shop.dto.response.PromotionResponse;
import com.example.shoozy_shop.dto.response.PromotionVariantDetailResponse;
import com.example.shoozy_shop.exception.PromotionException;
import com.example.shoozy_shop.model.*;
import com.example.shoozy_shop.repository.ProductVariantRepository;
import com.example.shoozy_shop.repository.PromotionProductRepository;
import com.example.shoozy_shop.repository.PromotionRepository;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PromotionService implements IPromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final ProductVariantRepository productVariantRepository;
    private static final double MAX_PERCENT = 90.0;


    @Override
    public PromotionDetailResponse getPromotionInfoById(Long id) throws Exception {
        PromotionResponse base = promotionRepository.findPromotionInfoById(id);
        List<Tuple> tuples = promotionProductRepository.findPromotionVariantTuplesByPromotionId(id);

        List<PromotionVariantDetailResponse> variantDetails = tuples.stream().map(tuple -> PromotionVariantDetailResponse.builder()
                .idProductVariant(tuple.get("idProductVariant", Long.class))
                .idProduct(tuple.get("idProduct", Long.class))
                .size(tuple.get("size", Integer.class))
                .color(tuple.get("color", String.class))
                .customValue(tuple.get("customValue", Double.class))
                .originalPrice(tuple.get("originalPrice", Double.class))
                .build()
        ).toList();

        return PromotionDetailResponse.builder()
                .id(base.getId())
                .name(base.getName())
                .code(base.getCode())
                .startDate(base.getStartDate())
                .expirationDate(base.getExpirationDate())
                .description(base.getDescription())
                .status(base.getStatus())
                .value(base.getValue())
                .promotionVariantDetailResponses(variantDetails)
                .build();
    }

    @Override
    public Promotion getPromotionById(Long id) throws Exception {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new Exception("Promotion not found with ID: " + id));
    }

    @Override
    public Page<PromotionResponse> getPromotionByPage(Pageable pageable,
                                                      String keyword,
                                                      LocalDate startDate,
                                                      LocalDate endDate,
                                                      Integer status) throws Exception {
        return promotionRepository.findPromotionInfoWithProductCount(
                keyword, startDate, endDate, status, pageable);
    }


    @Override
    public void deletePromotion(Long id) throws Exception {
        Promotion existingPromotion = getPromotionById(id);
        existingPromotion.setStatus(DefineStatus.DELETED.getValue());
        promotionRepository.save(existingPromotion);
    }

    @Transactional
    @Override
    public PromotionResponse createPromotion(PromotionRequest promotionRequest) throws Exception {
        if (promotionRequest.getProductPromotionRequests() == null || promotionRequest.getProductPromotionRequests().isEmpty()) {
            throw new IllegalArgumentException("Danh sách biến thể khuyến mãi không được để trống.");
        }
        if (!checkPromotionValid(promotionRequest)) {
            throw new IllegalArgumentException("PromotionRequest không hợp lệ.");
        }

        int status = DefineStatus.defineStatus(
                promotionRequest.getStartDate(),
                promotionRequest.getExpirationDate()

        ).getValue();

        String code = "SHOOZY" + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();

        Promotion promotion = Promotion.builder()
                .name(promotionRequest.getName())
                .value(promotionRequest.getValue())
                .startDate(promotionRequest.getStartDate())
                .expirationDate(promotionRequest.getExpirationDate())
                .code(code)
                .description(promotionRequest.getDescription())
                .status(status)
                .build();

        promotionRepository.save(promotion);

        setProductVariant(promotionRequest, promotion);

        return PromotionResponse.builder()
                .name(promotion.getName())
                .code(promotion.getCode())
                .startDate(promotion.getStartDate())
                .expirationDate(promotion.getExpirationDate())
                .status(promotion.getStatus())
                .description(promotion.getDescription())
                .value(promotion.getValue())
                .build();
    }

    @Transactional
    @Override
    public PromotionResponse updatePromotion(Long id, PromotionRequest promotionRequest) throws Exception {
        Promotion existingPromotion = promotionRepository.findById(id)
                .orElseThrow(() -> new Exception("Promotion not found with ID: " + id));
        if (!checkPromotionValid(promotionRequest)) {
            throw new IllegalArgumentException("PromotionRequest không hợp lệ.");
        }

        existingPromotion.setName(promotionRequest.getName());
        existingPromotion.setValue(promotionRequest.getValue());
        existingPromotion.setStartDate(promotionRequest.getStartDate());
        existingPromotion.setExpirationDate(promotionRequest.getExpirationDate());

        int status = DefineStatus.defineStatus(promotionRequest.getStartDate(), promotionRequest.getExpirationDate()).getValue();
        existingPromotion.setStatus(status);

        List<ProductPromotionRequest> requestList = promotionRequest.getProductPromotionRequests();
        if (requestList == null || requestList.isEmpty()) {
            throw new IllegalArgumentException("Danh sách biến thể khuyến mãi không được để trống.");
        }

        List<ProductPromotion> currentPromotions = promotionProductRepository.findByPromotionId(id);
        Set<Long> requestIds = requestList.stream()
                .map(ProductPromotionRequest::getIdPromotionProduct)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ProductPromotion> toDelete = currentPromotions.stream()
                .filter(pp -> !requestIds.contains(pp.getId()))
                .toList();
        promotionProductRepository.deleteAllInBatch(toDelete);

        for (ProductPromotionRequest ppq : requestList) {
            if (ppq.getProductVariantIds() == null) continue;

            for (Long variantId : ppq.getProductVariantIds()) {
                ProductVariant productVariant = productVariantRepository.findById(variantId)
                        .orElseThrow(() -> new Exception("Product variant not found with ID: " + variantId));

                ProductPromotion productPromotion = ppq.getIdPromotionProduct() != null
                        ? promotionProductRepository.findById(ppq.getIdPromotionProduct()).orElse(new ProductPromotion())
                        : new ProductPromotion();

                productPromotion.setPromotion(existingPromotion);
                productPromotion.setProductVariant(productVariant);
                productPromotion.setCustomValue(ppq.getCustomValue());

                promotionProductRepository.save(productPromotion);
            }
        }

        return PromotionResponse.builder()
                .name(existingPromotion.getName())
                .code(existingPromotion.getCode())
                .startDate(existingPromotion.getStartDate())
                .expirationDate(existingPromotion.getExpirationDate())
                .status(existingPromotion.getStatus())
                .value(existingPromotion.getValue())
                .description(existingPromotion.getDescription())
                .build();
    }

    private void setProductVariant(PromotionRequest promotionRequest, Promotion promotion) throws Exception {
        List<ProductPromotionRequest> productPromotionRequests = promotionRequest.getProductPromotionRequests();

        if (productPromotionRequests == null || productPromotionRequests.isEmpty()) {
            throw new IllegalArgumentException("Danh sách biến thể khuyến mãi không được để trống.");
        }

        for (ProductPromotionRequest ppq : productPromotionRequests) {
            if (ppq.getProductVariantIds() == null) continue;

            for (Long productVariantId : ppq.getProductVariantIds()) {
                ProductVariant productVariant = productVariantRepository.findById(productVariantId)
                        .orElseThrow(() -> new Exception("Product variant not found with ID: " + productVariantId));

                ProductPromotion productPromotion = ProductPromotion.builder()
                        .promotion(promotion)
                        .productVariant(productVariant)
                        .customValue(
                                ppq.getCustomValue() != null && !Objects.equals(ppq.getCustomValue(), promotion.getValue())
                                        ? ppq.getCustomValue() : null
                        )
                        .build();

                promotionProductRepository.save(productPromotion);
            }
        }
    }

    @Override
    public void activePromotion(Long id) throws Exception {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new Exception("Promotion not found with ID: " + id));

        LocalDate today = LocalDate.now();

        if (promotion.getStatus() != DefineStatus.DELETED.getValue()) {
            throw new Exception("Chỉ có thể khôi phục khuyến mãi đã bị xóa.");
        }

        LocalDate startDate = promotion.getStartDate().toLocalDate();
        LocalDate endDate = promotion.getExpirationDate() != null ? promotion.getExpirationDate().toLocalDate() : null;

        if (startDate.isAfter(today)) {
            promotion.setStatus(DefineStatus.UPCOMING.getValue());
        } else if ((startDate.isEqual(today) || startDate.isBefore(today))
                && (endDate == null || endDate.isAfter(today) || endDate.isEqual(today))) {
            promotion.setStatus(DefineStatus.LAUNCHING.getValue());
        } else {
            throw new Exception("Không thể khôi phục khuyến mãi đã hết hạn.");
        }

        promotionRepository.save(promotion);
    }
    private Boolean checkPromotionValid(PromotionRequest promotionRequest) throws Exception {
        // 1) kiểm tra cơ bản
        if (promotionRequest == null) {
            throw new PromotionException("PromotionRequest không được null.");
        }
        if (promotionRequest.getName() == null || promotionRequest.getName().isBlank()) {
            throw new PromotionException("Tên khuyến mãi không được để trống.");
        }
        if (promotionRequest.getValue() == null) {
            throw new PromotionException("Giá trị khuyến mãi không được null.");
        }
        if (promotionRequest.getValue() <= 0) {
            throw new PromotionException("Giá trị khuyến mãi phải lớn hơn 0.");
        }
        if (promotionRequest.getValue() > MAX_PERCENT) {
            throw new PromotionException("Giá trị khuyến mãi tối đa là " + MAX_PERCENT + "%.");
        }

        // 2) kiểm tra ngày bắt đầu / kết thúc
        if (promotionRequest.getStartDate() == null) {
            throw new PromotionException("Ngày bắt đầu là bắt buộc.");
        }
        if (promotionRequest.getExpirationDate() != null &&
                promotionRequest.getExpirationDate().isBefore(promotionRequest.getStartDate())) {
            throw new PromotionException("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu.");
        }

        // 3) kiểm tra danh sách biến thể
        List<ProductPromotionRequest> items = promotionRequest.getProductPromotionRequests();
        if (items == null || items.isEmpty()) {
            throw new PromotionException("Danh sách biến thể khuyến mãi không được để trống.");
        }

        // 4) kiểm tra id biến thể trùng lặp và customValue
        Set<Long> allIds = new HashSet<>();
        Set<Long> dupes = new HashSet<>();
        for (ProductPromotionRequest item : items) {
            if (item == null) continue;

            Double custom = item.getCustomValue();
            if (custom != null) {
                if (custom <= 0) {
                    throw new PromotionException("Custom value (nếu có) phải lớn hơn 0.");
                }
                if (custom > MAX_PERCENT) {
                    throw new PromotionException("Custom value tối đa là " + MAX_PERCENT + "%.");
                }
            }

            if (item.getProductVariantIds() == null) continue;
            for (Long id : item.getProductVariantIds()) {
                if (id == null) continue;
                if (!allIds.add(id)) dupes.add(id);
            }
        }

        if (!dupes.isEmpty()) {
            throw new PromotionException("Trùng lặp productVariantIds: " + dupes);
        }
        if (allIds.isEmpty()) {
            throw new PromotionException("Phải chọn ít nhất 1 biến thể sản phẩm.");
        }

        // 5) kiểm tra biến thể có tồn tại
        List<ProductVariant> found = productVariantRepository.findAllById(allIds);
        if (found.size() != allIds.size()) {
            Set<Long> foundIds = found.stream().map(ProductVariant::getId).collect(Collectors.toSet());
            Set<Long> missing = new HashSet<>(allIds);
            missing.removeAll(foundIds);
            throw new Exception("Không tìm thấy biến thể sản phẩm với ID: " + missing);
        }

        return true;
    }
}
