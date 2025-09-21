package com.example.shoozy_shop.clients;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GhnApiClient {

    // KHỚP YAML kebab-case
    @Value("${ghn.base-url:https://dev-online-gateway.ghn.vn/shiip/public-api}")
    private String baseUrl;

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shopId}")
    private String shopId;

    private final RestTemplate rt = new RestTemplate();

    @PostConstruct
    void checkProps() {
        Assert.hasText(baseUrl, "Missing ghn.base-url");
        Assert.hasText(token, "Missing ghn.token");
        Assert.hasText(shopId, "Missing ghn.shop-id");
        log.info("[GHN] baseUrl={}, shopId={}", baseUrl, shopId);
    }

    private HttpHeaders headers(boolean withShopId) {
        HttpHeaders h = new HttpHeaders();
        h.set("Token", token);                // KHÔNG dùng Bearer
        if (withShopId) h.set("ShopId", shopId);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /* --------------------- Helpers --------------------- */

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(ResponseEntity<Map> resp, String apiName) {
        try {
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("GHN {} HTTP {}", apiName, resp.getStatusCodeValue());
                return Collections.emptyList();
            }
            Map<String, Object> body = resp.getBody();
            if (body == null) {
                log.error("GHN {} trả body=null", apiName);
                return Collections.emptyList();
            }

            Object code = body.get("code"); // GHN thường có code=200
            if (code instanceof Number && ((Number) code).intValue() != 200) {
                log.error("GHN {} code={} msg={}", apiName, code, body.get("message"));
                return Collections.emptyList();
            }
            Object data = body.get("data");
            if (data instanceof List) return (List<Map<String, Object>>) data;

            // Không trả null để tránh NPE
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Lỗi khi xử lý dữ liệu {} từ GHN: {}", apiName, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(ResponseEntity<Map> resp, String apiName) {
        try {
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("GHN {} HTTP {}", apiName, resp.getStatusCodeValue());
                return Collections.emptyMap();
            }
            Map<String, Object> body = resp.getBody();
            if (body == null) {
                log.error("GHN {} trả body=null", apiName);
                return Collections.emptyMap();
            }

            Object code = body.get("code");
            if (code instanceof Number && ((Number) code).intValue() != 200) {
                log.error("GHN {} code={} msg={}", apiName, code, body.get("message"));
                return Collections.emptyMap();
            }
            Object data = body.get("data");
            if (data instanceof Map) return (Map<String, Object>) data;
            if (data == null) return Collections.emptyMap();

            log.error("GHN {} data không phải Map", apiName);
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Lỗi khi xử lý dữ liệu {} từ GHN: {}", apiName, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private static int toInt(Object o) {
        try {
            if (o instanceof Number) return ((Number) o).intValue();
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi giá trị {} sang số nguyên: {}", o, e.getMessage());
            return 0;
        }
    }

    /* --------------------- Master Data --------------------- */

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProvinces() {
        try {
            String url = baseUrl + "/master-data/province";
            var resp = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(headers(false)), Map.class);
            return extractList(resp, "province");
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách tỉnh/thành phố: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDistrictsByProvince(Integer provinceId) {
        try {
            String url = baseUrl + "/master-data/district";
            Map<String, Object> body = Map.of("province_id", provinceId);
            var resp = rt.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers(false)), Map.class);
            return extractList(resp, "district");
        } catch (Exception e) {
            log.error("Lỗi khi lấy districts cho provinceId={}: {}", provinceId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getWardsByDistrict(Integer districtId) {
        try {
            String url = baseUrl + "/master-data/ward?district_id=" + districtId;
            var resp = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(headers(false)), Map.class);
            return extractList(resp, "ward");
        } catch (Exception e) {
            log.error("Lỗi khi lấy wards cho districtId={}: {}", districtId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> availableServices(Integer fromDistrict, Integer toDistrict) {
        String url = baseUrl + "/v2/shipping-order/available-services";

        Map<String,Object> body = new HashMap<>();
        body.put("shop_id", Integer.parseInt(shopId));  // <-- THÊM
        body.put("from_district", fromDistrict);
        body.put("to_district", toDistrict);

        HttpHeaders h = headers(true);                  // đã có header ShopId
        try {
            var resp = rt.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
            return extractList(resp, "available-services");
        } catch (Exception ex) {
            log.error("Lỗi khi lấy dịch vụ vận chuyển từ district {} đến {}: {}", fromDistrict, toDistrict, ex.getMessage());
            throw ex;
        }
    }


    /* --------------------- Fee --------------------- */

    @SuppressWarnings("unchecked")
    public Integer calcFee(Integer fromDistrictId, Integer toDistrictId, String toWardCode,
                           Integer weight, Integer serviceTypeId, Integer insuranceValue) {
        String url = baseUrl + "/v2/shipping-order/fee";

        Map<String,Object> body = new HashMap<>();
        body.put("shop_id", Integer.parseInt(shopId));      // <-- THÊM
        body.put("from_district_id", fromDistrictId);
        body.put("to_district_id",   toDistrictId);
        body.put("to_ward_code",     toWardCode);           // CHUỖI
        body.put("weight",           weight);
        if (serviceTypeId != null)  body.put("service_type_id", serviceTypeId);
        if (insuranceValue != null) body.put("insurance_value", insuranceValue);

        HttpHeaders h = headers(true); // có "ShopId"
        try {
            var resp = rt.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
            Map<String,Object> data = extractMap(resp, "fee");
            Object total = data.get("total");
            if (total == null) total = data.get("service_fee");
            if (total == null) throw new IllegalStateException("GHN fee không có total/service_fee");
            return toInt(total);
        } catch (Exception ex) {
            log.error("Lỗi khi tính phí vận chuyển cho districtId={}, wardCode={}: {}", toDistrictId, toWardCode, ex.getMessage());
            throw ex;
        }
    }

}
