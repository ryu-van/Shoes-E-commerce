package com.example.shoozy_shop.dto.response;

import java.util.List;

public class AIResponse {
    private String type;       // "product", "promotion", "text", "policy", "contact"
    private String message;    // câu trả lời tự nhiên
    private Object data;       // dữ liệu (có thể là List<?> hoặc Object)
    private Integer totalCount; // tổng số kết quả

    public AIResponse() {}

    public AIResponse(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public AIResponse(String type, String message, Object data) {
        this.type = type;
        this.message = message;
        this.data = data;
        this.totalCount = (data instanceof List<?>) ? ((List<?>) data).size() : null;
    }

    // Getters & Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) {
        this.data = data;
        this.totalCount = (data instanceof List<?>) ? ((List<?>) data).size() : null;
    }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }



}

