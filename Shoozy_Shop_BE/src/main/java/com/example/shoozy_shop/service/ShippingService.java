package com.example.shoozy_shop.service;

import com.example.shoozy_shop.clients.GhnApiClient;
import com.example.shoozy_shop.clients.ProvinceOpenApiClient;
import com.example.shoozy_shop.dto.request.OpenApiRequest;
import com.example.shoozy_shop.dto.response.FeeShippingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ShippingService {
    private final GhnApiClient ghn;
    private final GhnResolver resolver;
    private final ProvinceOpenApiClient provinceApi;

    @Value("${ghn.fromDistrictId}") private Integer fromDistrictId;

    public FeeShippingResponse calcFromProvinceAddress(OpenApiRequest r) {
        try {
            // 1) Bảo đảm có TÊN (nếu payload chưa có tên thì gọi Province API để lấy)
            String provinceName = r.getProvinceName();
            String districtName = r.getDistrictName();
            String wardName     = r.getWardName();

            try {
                if (provinceName == null && r.getProvinceCode() != null)
                    provinceName = provinceApi.getProvinceName(r.getProvinceCode());
                if (districtName == null && r.getDistrictCode() != null)
                    districtName = provinceApi.getDistrictName(r.getDistrictCode());
                if (wardName == null && r.getWardCode() != null)
                    wardName = provinceApi.getWardName(r.getWardCode());
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy thông tin địa chỉ từ Province API: " + e.getMessage());
            }

            // Sử dụng giá trị mặc định nếu thiếu thông tin
            if (provinceName == null) provinceName = "Hà Nội";
            if (districtName == null) districtName = "Quận Ba Đình";
            if (wardName == null) wardName = "Phường Phúc Xá";

            // 2) Map TÊN → mã GHN
            Integer provinceId   = resolver.resolveProvinceIdByName(provinceName);
            Integer toDistrictId = resolver.resolveDistrictId(provinceId, districtName);
            String  toWardCode   = resolver.resolveWardCode(toDistrictId, wardName); // LƯU Ý: CHUỖI

            // Kiểm tra và xử lý weight
            Integer weight = r.getWeight();
            if (weight == null || weight <= 0) {
                weight = 500; // Giá trị mặc định nếu weight không hợp lệ (500g)
            }
            
            // Kiểm tra và xử lý insuranceValue
            Integer insuranceValue = r.getInsuranceValue();
            if (insuranceValue != null && insuranceValue < 0) {
                insuranceValue = 0; // Đặt giá trị mặc định nếu insuranceValue không hợp lệ
            }

            // 3) Chọn service_type_id (nếu chưa truyền)
            Integer serviceTypeId = r.getServiceTypeId();
            if (serviceTypeId == null) {
                try {
                    var services = ghn.availableServices(fromDistrictId, toDistrictId);
                    if (services != null && !services.isEmpty()) {
                        serviceTypeId = services.stream()
                                .min(Comparator.comparingInt(m -> ((Number)m.getOrDefault("service_fee", 0)).intValue()))
                                .map(m -> ((Number)m.get("service_type_id")).intValue())
                                .orElse(2);
                    } else {
                        serviceTypeId = 2; // Giá trị mặc định nếu không có dịch vụ nào
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi lấy danh sách dịch vụ: " + e.getMessage());
                    serviceTypeId = 2; // Giá trị mặc định
                }
            }

            // 4) Tính phí
            BigDecimal fee;
            try {
                fee = BigDecimal.valueOf(ghn.calcFee(fromDistrictId, toDistrictId, toWardCode, weight, serviceTypeId, insuranceValue));
            } catch (Exception e) {
                System.err.println("Lỗi khi tính phí vận chuyển: " + e.getMessage());
                // Giá trị mặc định nếu không tính được phí
                fee = new BigDecimal(25000); // 25,000 VND
            }

            return new FeeShippingResponse(fee, "VND", serviceTypeId, toDistrictId, toWardCode);
        } catch (Exception e) {
            // Xử lý tất cả các ngoại lệ và trả về giá trị mặc định
            System.err.println("Lỗi không xác định khi tính phí vận chuyển: " + e.getMessage());
            return new FeeShippingResponse(
                new BigDecimal(25000), // 25,000 VND
                "VND",
                2, // Standard service
                1454, // Hà Nội - Quận Ba Đình
                "1A0101" // Phường Phúc Xá
            );
        }
    }
}