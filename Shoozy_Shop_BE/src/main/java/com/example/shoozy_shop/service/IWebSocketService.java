package com.example.shoozy_shop.service;

public interface IWebSocketService {
    void broadcastRefresh(String type, Object data, String action);
}