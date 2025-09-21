package com.example.shoozy_shop.service;

import com.example.shoozy_shop.clients.GhnApiClient;
import com.example.shoozy_shop.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GhnResolver {
    private final GhnApiClient ghn;

    /* -------- Helpers: chuẩn hoá & match mềm -------- */

    private static String norm(String s) {
        return TextNormalizer.norm(s); // đã bỏ dấu, hạ chữ, bỏ tiền tố Quận/Phường...
    }

    /** match mềm: candidate chứa đủ các token của target (hoặc bằng/startsWith) */
    private static boolean tokenMatch(String candidate, String target) {
        String c = norm(candidate), t = norm(target);
        if (c.equals(t) || c.startsWith(t) || t.startsWith(c)) return true;
        for (String tk : t.split(" "))
            if (!tk.isBlank() && !c.contains(tk)) return false;
        return true;
    }

    /** match theo tên chính + NameExtension (nếu có) */
    private static boolean matchWithExtensions(Map<String,Object> row, String input, String nameKey, String extKey) {
        if (tokenMatch(Objects.toString(row.get(nameKey), ""), input)) return true;
        Object ext = row.get(extKey);
        if (ext instanceof List<?> list) {
            for (Object o : list) {
                if (tokenMatch(Objects.toString(o, ""), input)) return true;
            }
        }
        return false;
    }

    /* ------------------- Resolve IDs ------------------- */

    public Integer resolveProvinceIdByName(String provinceName) {
        try {
            var list = ghn.getProvinces();
            if (list == null || list.isEmpty()) {
                // Nếu không lấy được danh sách tỉnh, sử dụng giá trị mặc định
                System.err.println("GHN: provinces rỗng. Kiểm tra token/host.");
                // Hà Nội (default)
                return 201;
            }

            return list.stream()
                    .filter(p -> tokenMatch(Objects.toString(p.get("ProvinceName"), ""), provinceName))
                    .map(p -> ((Number) p.get("ProvinceID")).intValue())
                    .findFirst()
                    .orElse(201); // Nếu không tìm thấy, sử dụng Hà Nội làm mặc định
        } catch (Exception e) {
            // Log lỗi và sử dụng giá trị mặc định
            System.err.println("Lỗi khi lấy province từ GHN: " + e.getMessage());
            // Hà Nội (default)
            return 201;
        }
    }

    public Integer resolveDistrictId(Integer provinceId, String districtName) {
        try {
            var list = ghn.getDistrictsByProvince(provinceId);
            if (list == null || list.isEmpty()) {
                // Nếu provinceId không hợp lệ hoặc không có districts, sử dụng giá trị mặc định
                System.err.println("GHN: districts rỗng cho provinceId=" + provinceId);
                // Hà Nội - Quận Ba Đình (default)
                return 1454;
            }

            return list.stream()
                    .filter(d -> matchWithExtensions(d, districtName, "DistrictName", "NameExtension"))
                    .map(d -> ((Number) d.get("DistrictID")).intValue())
                    .findFirst()
                    .orElse(1454); // Nếu không tìm thấy, sử dụng Quận Ba Đình làm mặc định
        } catch (Exception e) {
            // Log lỗi và sử dụng giá trị mặc định
            System.err.println("Lỗi khi lấy district từ GHN: " + e.getMessage());
            // Hà Nội - Quận Ba Đình (default)
            return 1454;
        }
    }

    public String resolveWardCode(Integer districtId, String wardName) {
        try {
            var wards = ghn.getWardsByDistrict(districtId);
            if (wards == null || wards.isEmpty()) {
                // Nếu districtId không hợp lệ hoặc không có wards, sử dụng giá trị mặc định
                System.err.println("GHN: wards rỗng cho districtId=" + districtId);
                // Phường Phúc Xá, Quận Ba Đình, Hà Nội (default)
                return "1A0101";
            }

            return wards.stream()
                    .filter(w -> matchWithExtensions(w, wardName, "WardName", "NameExtension"))
                    .map(w -> Objects.toString(w.get("WardCode"), null))
                    .findFirst()
                    .orElse("1A0101"); // Nếu không tìm thấy, sử dụng Phường Phúc Xá làm mặc định
        } catch (Exception e) {
            // Log lỗi và sử dụng giá trị mặc định
            System.err.println("Lỗi khi lấy ward từ GHN: " + e.getMessage());
            // Phường Phúc Xá, Quận Ba Đình, Hà Nội (default)
            return "1A0101";
        }
    }
}
