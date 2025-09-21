package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.response.InvoiceResponseDTO;
import com.example.shoozy_shop.dto.response.OrderItemResponseDTO;
import com.example.shoozy_shop.model.Order;
import com.example.shoozy_shop.repository.*;
import com.example.shoozy_shop.util.EmailContentGenerator;
import com.example.shoozy_shop.util.InvoicePdfGenerator;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private JavaMailSender mailSender;

    // Lấy toàn bộ hóa đơn
    public List<InvoiceResponseDTO> getAllOrders() {
        logger.info("Fetching all invoices with status 'COMPLETED' or 'cancelled'");
        List<Order> orders = invoiceRepository.findAllByStatusIn(List.of("COMPLETED", "cancelled"));
        return orders.stream().map(this::mapToInvoiceResponseDTO).collect(Collectors.toList());
    }

    // Lấy hóa đơn theo ID
    public InvoiceResponseDTO getInvoiceById(Long orderId) {
        logger.info("Fetching invoice with ID: {}", orderId);
        Order order = invoiceRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại"));
        return mapToInvoiceResponseDTO(order);
    }

    // Gửi hóa đơn qua email
    public void sendInvoiceEmail(Long orderId, String recipientEmail) {
        logger.info("Sending invoice email for order ID: {} to {}", orderId, recipientEmail);
        InvoiceResponseDTO invoice = getInvoiceById(orderId);

        try {
            // Tạo file PDF
            byte[] pdfBytes = InvoicePdfGenerator.generateInvoicePdf(invoice);

            // Tạo nội dung email HTML
            String emailContent = EmailContentGenerator.generateEmailContent(invoice);

            // Gửi email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("Hóa đơn #" + invoice.getOrderId() + " từ Shoozy Shop");
            helper.setText(emailContent, true);

            // Sử dụng ByteArrayDataSource để đính kèm file PDF
            ByteArrayDataSource dataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
            dataSource.setName("HoaDon_" + invoice.getOrderId() + ".pdf");
            helper.addAttachment("HoaDon_" + invoice.getOrderId() + ".pdf", dataSource);

            mailSender.send(mimeMessage);
            logger.info("Email sent successfully for order ID: {}", orderId);
        } catch (Exception e) {
            logger.error("Error sending email for order ID: {}", orderId, e);
            throw new RuntimeException("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    // Ánh xạ Order sang InvoiceResponseDTO
    private InvoiceResponseDTO mapToInvoiceResponseDTO(Order order) {
        InvoiceResponseDTO invoice = new InvoiceResponseDTO();
        invoice.setOrderId(order.getId());
        invoice.setOrderCode(order.getOrderCode());
        invoice.setOrderDate(order.getCreatedAt());
        invoice.setStatus(order.getStatus());
        invoice.setCustomerName(order.getFullname());
        invoice.setPhoneNumber(order.getPhoneNumber());
        invoice.setEmail(order.getUser().getEmail());
        invoice.setShippingAddress(order.getAddress());
        invoice.setNote(order.getNote() != null ? order.getNote() : "");
        invoice.setTotalMoney(order.getTotalMoney());
        invoice.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().getName() : "N/A");
        invoice.setShopName("Shoozy Shop");
        invoice.setShopTaxCode("123456789");
        invoice.setShopAddress("123 Đường ABC, TP. HCM");
        invoice.setShopPhone("0123 456 789");

        // Map coupon
        if (order.getCoupon() != null) {
            invoice.setCouponName(order.getCoupon().getName());
            invoice.setCouponDescription(order.getCoupon().getDescription());
            invoice.setCouponValue(order.getCoupon().getValue());
        }

        // Map order details
        List<OrderItemResponseDTO> items = order.getOrderDetails().stream()
                .map(detail -> {
                    OrderItemResponseDTO item = new OrderItemResponseDTO();
                    item.setProductName(detail.getProductVariant().getProduct().getName());
                    item.setSize(detail.getProductVariant().getSize().getValue());
                    item.setColor(detail.getProductVariant().getColor().getName());
                    item.setThumbnail(detail.getProductVariant().getThumbnail());
                    item.setQuantity(detail.getQuantity());
                    item.setPrice(detail.getPrice());
                    item.setTotalMoney(detail.getTotalMoney());
                    return item;
                })
                .collect(Collectors.toList());
        invoice.setItems(items);

        // Tính tổng tiền hàng
        double totalItems = items.stream().mapToDouble(OrderItemResponseDTO::getTotalMoney).sum();
        invoice.setTotalItems(totalItems);

        return invoice;
    }
}