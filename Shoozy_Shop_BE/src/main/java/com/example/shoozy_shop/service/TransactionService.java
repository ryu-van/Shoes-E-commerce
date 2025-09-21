package com.example.shoozy_shop.service;

import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.model.PaymentMethod;
import com.example.shoozy_shop.model.Transaction;
import com.example.shoozy_shop.repository.OrderRepository;
import com.example.shoozy_shop.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TransactionService implements ITransactionService {

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;

    private static final Long METHOD_CASH_ID = 1L; // CASH
    private static final Long METHOD_ONL_ID  = 2L; // ONL

    private static final String PENDING   = "PENDING";
    private static final String SUCCESS   = "SUCCESS";
    private static final String CANCELLED = "CANCELLED";

    private boolean isCash(PaymentMethod pm) { return pm != null && METHOD_CASH_ID.equals(pm.getId()); }
    private boolean isOnl (PaymentMethod pm) { return pm != null && METHOD_ONL_ID .equals(pm.getId()); }

    private boolean isCOD(Order o) {
        return o != null && Boolean.TRUE.equals(o.getType()) && isCash(o.getPaymentMethod());
    }
    private boolean isVnPayOrder(Order o) {
        return o != null && Boolean.TRUE.equals(o.getType()) && isOnl(o.getPaymentMethod());
    }
    private boolean isCashAtCounter(Order o) {
        return o != null && !Boolean.TRUE.equals(o.getType()) && isCash(o.getPaymentMethod());
    }
    private boolean isBankTransferAtCounter(Order o) {
        return o != null && !Boolean.TRUE.equals(o.getType()) && isOnl(o.getPaymentMethod());
    }

    @Override
    public Transaction ensureInitialCashTransaction(Order order) {
        if (order == null) throw new IllegalArgumentException("Order cannot be null");
        if (order.getFinalPrice() == null) throw new IllegalArgumentException("Final price is null.");
        if (order.getPaymentMethod() == null) throw new IllegalArgumentException("Payment method cannot be null");

        if (isVnPayOrder(order)) return null;

        cancelPendingTransactions(order.getId());

        BigDecimal amount = order.getFinalPrice().setScale(0, java.math.RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();

        String prefix;
        String status;
        LocalDateTime completed = null;

        if (isCOD(order)) {
            prefix = "COD";
            status = PENDING;
        } else if (isCashAtCounter(order)) {
            prefix = "CASH";
            status = SUCCESS;
            completed = now;
        } else if (isBankTransferAtCounter(order)) {
            prefix = "BANK";
            status = SUCCESS;
            completed = now;
        } else {
            prefix = "OFF";
            status = PENDING;
        }

        Transaction tx = Transaction.builder()
                .order(order)
                .amount(amount)
                .transactionCode(genInternalTxCode(prefix))
                .status(status)
                .transactionDate(now)
                .completedDate(completed)
                .build();

        return transactionRepository.save(tx);
    }

    // ---- VNPAY: chỉ gọi khi /payments/create-vnpay ----
    @Override
    public Transaction createOrRenewVnPayTransaction(Order order) {
        if (!isVnPayOrder(order)) {
            throw new IllegalStateException("Not a VNPAY order (ONLINE + ONL).");
        }
        if (order.getFinalPrice() == null) {
            throw new IllegalArgumentException("Final price is null.");
        }

        cancelPendingTransactions(order.getId());

        BigDecimal amount = order.getFinalPrice().setScale(0, java.math.RoundingMode.HALF_UP);
        String txnRef = "VN" + System.currentTimeMillis(); // dùng làm vnp_TxnRef

        Transaction tx = Transaction.builder()
                .order(order)
                .amount(amount)      // VNĐ, chưa x100
                .transactionCode(txnRef)
                .status(PENDING)     // chờ IPN
                .transactionDate(LocalDateTime.now())
                .build();

        return transactionRepository.save(tx);
    }

    // ---- Helpers chung ----
    private void cancelPendingTransactions(Long orderId) {
        List<Transaction> pendings = transactionRepository.findByOrderId(orderId).stream()
                .filter(t -> PENDING.equals(t.getStatus()))
                .toList();
        if (!pendings.isEmpty()) {
            pendings.forEach(t -> t.setStatus(CANCELLED));
            transactionRepository.saveAll(pendings);
        }
    }

    private String genInternalTxCode(String prefix) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int rand = (int) (Math.random() * 900) + 100;
        return prefix + ts + rand; // ví dụ: COD250815101530123
    }
}
