package com.example.shoozy_shop.service;

import com.example.shoozy_shop.config.VnpayConfig;
import com.example.shoozy_shop.dto.request.VnpayRequest;
import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.model.Transaction;
import com.example.shoozy_shop.model.VnPayTransactionDetail;
import com.example.shoozy_shop.repository.OrderRepository;
import com.example.shoozy_shop.repository.TransactionRepository;
import com.example.shoozy_shop.repository.VnPayTransactionDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
@Service
public class VnPayService {

    private final VnPayTransactionDetailRepository vnPayTransactionDetailRepository;
    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    // VnPayService.java
    @Transactional
    public boolean handleVnpayReturn(Map<String, String> vnpParams, String receivedHash, String requestUrl, String rawQuery) {
        String vnp_TxnRef = vnpParams.get("vnp_TxnRef");
        String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");
        String vnp_TransactionStatus = vnpParams.get("vnp_TransactionStatus");

        Transaction transaction = transactionRepository.findByTransactionCode(vnp_TxnRef);
        if (transaction == null) {
            throw new ResourceNotFoundException("Transaction not found with code: " + vnp_TxnRef);
        }

        if ("SUCCESS".equalsIgnoreCase(transaction.getStatus())) {
            throw new IllegalStateException("Already confirmed");
        }

        VnPayTransactionDetail detail = VnPayTransactionDetail.builder()
                .transaction(transaction)
                .vnpTxnRef(vnp_TxnRef)
                .vnpTransactionNo(vnpParams.get("vnp_TransactionNo"))
                .vnpBankCode(vnpParams.get("vnp_BankCode"))
                .vnpCardType(vnpParams.get("vnp_CardType"))
                .vnpPayDate(vnpParams.get("vnp_PayDate"))
                .vnpOrderInfo(vnpParams.get("vnp_OrderInfo"))
                .vnpResponseCode(vnp_ResponseCode)
                .vnpTransactionStatus(vnp_TransactionStatus)
                .requestUrl(requestUrl)
                .returnData(rawQuery)
                .secureHash(receivedHash)
                .build();
        vnPayTransactionDetailRepository.save(detail);

        long amountFromVnp = Long.parseLong(vnpParams.get("vnp_Amount"));
        long exportedAmount = transaction.getAmount()
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .longValueExact();
        if (amountFromVnp != exportedAmount) {
            throw new IllegalArgumentException("Mismatched amount: expected " + exportedAmount + ", got " + amountFromVnp);
        }

        if ("00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus)) {
            transaction.setStatus("SUCCESS");
            transaction.setCompletedDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            Order order = transaction.getOrder();
            if (order.getType()) {
                order.setStatus("PENDING");
                orderRepository.save(order);
            }
            else {
                order.setStatus("COMPLETED");
                orderRepository.save(order);
            }
            return true;
        } else {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            return false;
        }
    }

    public String createPayment(BigDecimal amountVnd, String txnRef, String clientIp,String returnUrl)
            throws UnsupportedEncodingException {

        if (amountVnd == null || amountVnd.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        // VNPAY yêu cầu x100 & không có phần thập phân
        String vnp_Amount = amountVnd
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .toPlainString();

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        String vnp_TxnRef = txnRef;
        String vnp_IpAddr = (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp;
        String vnp_TmnCode = VnpayConfig.vnp_TmnCode;

        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = VnpayConfig.vnp_ReturnUrl;
        }


        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", vnp_Amount);
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        // Tuỳ chọn preselect bank sandbox:
        // vnp_Params.put("vnp_BankCode", "NCB");

        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh"); // hoặc ZoneId.of("GMT+7")
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String createDate = LocalDateTime.now(vn).format(fmt);
        String expireDate = LocalDateTime.now(vn).plusMinutes(15).format(fmt);

        vnp_Params.put("vnp_CreateDate", createDate);
        vnp_Params.put("vnp_ExpireDate", expireDate);




        List<String> keys = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(keys);

        List<String> hashParts  = new ArrayList<>();
        List<String> queryParts = new ArrayList<>();

        for (String k : keys) {
            String v = vnp_Params.get(k);
            if (v == null || v.isEmpty()) continue;
            String encName  = URLEncoder.encode(k, StandardCharsets.US_ASCII);
            String encValue = URLEncoder.encode(v, StandardCharsets.US_ASCII);

            hashParts.add(k + "=" + encValue);              // tên gốc + value encode
            queryParts.add(encName + "=" + encValue);       // tên encode + value encode
        }

        String hashData = String.join("&", hashParts);
        String query    = String.join("&", queryParts);

        String vnp_SecureHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData);
        query += "&vnp_SecureHash=" + vnp_SecureHash;

        return VnpayConfig.vnp_PayUrl + "?" + query;

    }

    @Transactional
    public String retryVnPay(String orderCode, String clientIp, String returnUrl) throws UnsupportedEncodingException {
        // 1) Load order
        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Order không tồn tại: " + orderCode);
        }

        // 2) Chặn nếu đã thanh toán xong (ưu tiên equalsIgnoreCase, tốt nhất dùng enum)
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng đã bị hủy, không thể retry.");
        }
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng đã thanh toán thành công, không cần retry.");
        }


        // 3) Tìm transaction PENDING gần nhất (TTL 15 phút)
        final long TTL_MINUTES = 15L;
        Transaction pending = transactionRepository.findTopByOrder_OrderCodeAndStatusOrderByCreatedAtDesc(order.getOrderCode(), "PENDING").orElse(null);

        // Chuẩn bị amount
        BigDecimal amountVnd = order.getFinalPrice();
        if (amountVnd == null || amountVnd.signum() <= 0) {
            throw new IllegalStateException("Final price chưa sẵn sàng hoặc không hợp lệ.");
        }
        amountVnd = amountVnd.setScale(0, java.math.RoundingMode.HALF_UP);

        // 4) Nếu có PENDING còn hiệu lực → reuse txnRef cũ
        if (pending != null) {
            LocalDateTime created = pending.getCreatedAt();
            boolean withinTTL = (created != null) && !created.isBefore(LocalDateTime.now().minusMinutes(TTL_MINUTES));
            if (withinTTL) {
                return createPayment(amountVnd, pending.getTransactionCode(),
                        (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp,
                        returnUrl);
            }

            pending.setStatus("EXPIRED");
            pending.setNote("Expired by retry");
            transactionRepository.save(pending);
        }

        // 5) Không có PENDING hợp lệ → tạo transaction mới
        Transaction newTx = new Transaction();
        newTx.setOrder(order);
        newTx.setStatus("PENDING");
        newTx.setAmount(amountVnd);
        newTx.setTransactionCode(generateTxnRef(order));
        newTx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(newTx);

        // 6) Build URL thanh toán và trả về
        return createPayment(amountVnd, newTx.getTransactionCode(),
                (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp,
                returnUrl);
    }

    // Tạo mã giao dịch mới: ORDCODE-8HEX (ví dụ)
    private String generateTxnRef(Order order) {
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return order.getOrderCode() + "-" + rand;
    }





}
