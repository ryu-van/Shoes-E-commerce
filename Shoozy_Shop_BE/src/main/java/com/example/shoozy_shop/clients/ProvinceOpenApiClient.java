package com.example.shoozy_shop.clients;


import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class ProvinceOpenApiClient {
    private static final String BASE = "https://provinces.open-api.vn/api";
    private final RestTemplate rt = new RestTemplate();

    @SuppressWarnings("unchecked")
    private Map<String,Object> get(String path) {
        return rt.exchange(BASE + path, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Map.class).getBody();
    }

    public String getProvinceName(Integer code) {
        return String.valueOf(get("/p/" + code).get("name"));
    }
    public String getDistrictName(Integer code) {
        return String.valueOf(get("/d/" + code).get("name"));
    }
    public String getWardName(Integer code) {
        return String.valueOf(get("/w/" + code).get("name"));
    }
}
