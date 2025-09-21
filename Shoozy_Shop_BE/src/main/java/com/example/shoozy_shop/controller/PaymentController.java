package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.config.VnpayConfig;
import com.example.shoozy_shop.dto.request.OrderRequest;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.model.Transaction;
import com.example.shoozy_shop.repository.TransactionRepository;
import com.example.shoozy_shop.service.ITransactionService;
import com.example.shoozy_shop.service.OrderService;
import com.example.shoozy_shop.service.VnPayService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/payments")
public class PaymentController {
    private final VnPayService vnPayService;
    private final OrderService orderService;
    private final TransactionRepository transactionRepository;
    private final ITransactionService transactionService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Value("${api.prefix}")
    private String apiPrefix;


    @PostMapping("/create-vnpay")
    public ResponseEntity<?> createVnPayPayment(@RequestBody OrderRequest orderRequest,
                                                HttpServletRequest request) {
        try {
            // ĐÚNG QUY ƯỚC: ONLINE + ONL
            orderRequest.setType(true);
            if (orderRequest.getPaymentMethodId() == null || orderRequest.getPaymentMethodId() != 2L) {
                throw new IllegalArgumentException("Payment method must be ONL (id=2) for VNPAY.");
            }

            Order order = orderService.addOrder(orderRequest);

            Transaction tx  = transactionService.createOrRenewVnPayTransaction(order);
            BigDecimal finalPrice = order.getFinalPrice();
            if (finalPrice == null) {
                throw new IllegalArgumentException("Final price is null. Hãy tính và set finalPrice trước khi tạo thanh toán.");
            }

            BigDecimal amountVnd = finalPrice.setScale(0, RoundingMode.HALF_UP);
            String clientIp = getClientIp(request);
            String returnUrl = backendUrl + apiPrefix + "/payments/vnpay-return";
            String paymentUrl = vnPayService.createPayment(amountVnd, tx.getTransactionCode(), clientIp,returnUrl);
            return ResponseEntity.ok(ApiResponse.success("Tạo thanh toán VNPAY thành công", paymentUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Create VNPAY failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Create VNPAY failed: " + e.getMessage()));
        }
    }


    private String getClientIp(HttpServletRequest req) {
        String[] headers = {
                "X-Forwarded-For","Proxy-Client-IP","WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR","HTTP_X_FORWARDED","HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP","HTTP_FORWARDED_FOR","HTTP_FORWARDED","HTTP_VIA","REMOTE_ADDR"
        };
        for (String h: headers) {
            String ip = req.getHeader(h);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return req.getRemoteAddr();
    }


    // PaymentController.java
    @GetMapping("/vnpay-return")
    public void vnpReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> p = extractParams(request);
        String sig = p.remove("vnp_SecureHash");
        p.remove("vnp_SecureHashType");

        String data = VnpayConfig.buildHashData(p);
        boolean valid = Objects.equals(sig, VnpayConfig.hmacSHA512(VnpayConfig.secretKey, data));

        String txnRef = p.get("vnp_TxnRef");                 // = transactionCode
        String resp = p.get("vnp_ResponseCode");
        String trans = p.getOrDefault("vnp_TransactionStatus", "99");

        // ✅ Map txnRef -> orderCode để FE luôn nhận được orderCode đúng
        String orderCodeForRedirect = txnRef; // fallback
        try {
            Transaction tx = transactionRepository.findByTransactionCode(txnRef);
            if (tx != null && tx.getOrder() != null && tx.getOrder().getOrderCode() != null) {
                orderCodeForRedirect = tx.getOrder().getOrderCode();
            }
        } catch (Exception ignore) {}

        java.util.function.Function<String,String> enc = s -> URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);

        if (!valid) {
            response.sendRedirect(frontendUrl + "/orders/failed?orderCode=" + enc.apply(orderCodeForRedirect)
                    + "&code=97&msg=" + enc.apply("Chữ ký không hợp lệ"));
            return;
        }

        if ("00".equals(resp) && "00".equals(trans)) {
            try {
                // Fallback update nếu IPN chưa tới
                vnPayService.handleVnpayReturn(p, sig, request.getRequestURL().toString(), request.getQueryString());
            } catch (Exception ignore) {}
            response.sendRedirect(frontendUrl + "/orders/success?orderCode=" + enc.apply(orderCodeForRedirect));
            return;
        }

        String reason = switch (resp) {
            case "24" -> "Bạn đã hủy thanh toán.";
            case "09" -> "Giao dịch đang xử lý.";
            case "51" -> "Tài khoản không đủ số dư.";
            default -> "Thanh toán chưa hoàn thành.";
        };
        response.sendRedirect(frontendUrl + "/orders/failed?orderCode=" + enc.apply(orderCodeForRedirect)
                + "&code=" + enc.apply(resp == null ? "99" : resp)
                + "&msg=" + enc.apply(reason));
    }



    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String,String>> vnpIpn(HttpServletRequest request) {
        Map<String,String> p = extractParams(request);
        String sig = p.remove("vnp_SecureHash");
        p.remove("vnp_SecureHashType");

        String data = VnpayConfig.buildHashData(p);
        String calc = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, data);

        if (!Objects.equals(sig, calc)) {
            return ResponseEntity.ok(Map.of("RspCode","97","Message","Invalid signature"));
        }

        try {
            boolean ok = vnPayService.handleVnpayReturn(p, sig,
                    request.getRequestURL().toString(), request.getQueryString());
            if (ok) {
                return ResponseEntity.ok(Map.of("RspCode","00","Message","Confirm Success"));
            } else {
                return ResponseEntity.ok(Map.of("RspCode","99","Message","Confirm Fail"));
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.ok(Map.of("RspCode","01","Message","Order/Tx not found"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("RspCode","02","Message","Invalid amount"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("RspCode","04","Message","Order already confirmed"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("RspCode","99","Message","Unknown error"));
        }
    }

    @PostMapping("/retry-vnpay/{orderCode}")
    public ResponseEntity<?> retryVnpay(@PathVariable String orderCode, HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            String returnUrl = backendUrl + apiPrefix + "/payments/vnpay-return";
            String paymentUrl = vnPayService.retryVnPay(orderCode, clientIp, returnUrl);
            return ResponseEntity.ok(ApiResponse.success("Retry VNPAY OK", paymentUrl));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Retry failed: " + e.getMessage()));
        }
    }



    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }




}
