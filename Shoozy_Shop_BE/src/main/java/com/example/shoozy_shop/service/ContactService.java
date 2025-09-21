package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.ContactRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.admin.email:shopshoozy@gmail.com}")
    private String adminEmail;

    public void sendContactEmail(ContactRequest contactRequest) {
        try {
            // Gửi email cho admin
            sendEmailToAdmin(contactRequest);

            // Gửi email xác nhận cho khách hàng
            sendConfirmationEmailToCustomer(contactRequest);

            log.info("Contact emails sent successfully for: {}", contactRequest.getEmail());
        } catch (Exception e) {
            log.error("Failed to send contact email: ", e);
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }

    private void sendEmailToAdmin(ContactRequest contactRequest) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(adminEmail);
        helper.setSubject("Liên hệ mới từ website - " + contactRequest.getFullName());

        // Tạo nội dung HTML đơn giản
        String htmlContent = createAdminEmailTemplate(
                contactRequest.getFullName(),
                contactRequest.getEmail(),
                contactRequest.getMessage(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );
        helper.setText(htmlContent, true);

        emailSender.send(message);
    }

    private void sendConfirmationEmailToCustomer(ContactRequest contactRequest) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(contactRequest.getEmail());
        helper.setSubject("Xác nhận liên hệ - Shoozy Shop");

        String htmlContent = createCustomerConfirmationTemplate(
                contactRequest.getFullName(),
                contactRequest.getMessage()
        );
        helper.setText(htmlContent, true);

        emailSender.send(message);
    }

    // Tạo template email cho admin
    private String createAdminEmailTemplate(String fullName, String email, String message, String dateTime) {
        return String.format("""
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
                    .content { background: #fff; padding: 20px; border: 1px solid #dee2e6; border-radius: 8px; }
                    .info-row { margin-bottom: 15px; }
                    .label { font-weight: bold; color: #495057; }
                    .message-box { background: #f8f9fa; padding: 15px; border-radius: 5px; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🔔 Liên hệ mới từ website</h2>
                    </div>
                    <div class="content">
                        <div class="info-row">
                            <span class="label">👤 Họ tên:</span> %s
                        </div>
                        <div class="info-row">
                            <span class="label">📧 Email:</span> %s
                        </div>
                        <div class="info-row">
                            <span class="label">🕒 Thời gian:</span> %s
                        </div>
                        <div class="message-box">
                            <div class="label">💬 Nội dung tin nhắn:</div>
                            <p>%s</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, fullName, email, dateTime, message);
    }

    private String createCustomerConfirmationTemplate(String fullName, String message) {
        return String.format("""
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; border-radius: 8px; text-align: center; }
                    .content { background: #fff; padding: 30px; border: 1px solid #dee2e6; border-radius: 8px; margin-top: 20px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Cảm ơn bạn đã liên hệ!</h1>
                        <p>Shoozy Shop</p>
                    </div>
                    <div class="content">
                        <p>Xin chào <strong>%s</strong>,</p>
                        <p>Chúng tôi đã nhận được tin nhắn của bạn và sẽ phản hồi trong thời gian sớm nhất (thường trong vòng 24 giờ).</p>
                        
                        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <h3>📝 Nội dung tin nhắn của bạn:</h3>
                            <p style="font-style: italic;">"%s"</p>
                        </div>
                        
                        <p>Nếu bạn có thêm câu hỏi nào khác, đừng ngần ngại liên hệ với chúng tôi!</p>
                        
                        <p>Trân trọng,<br><strong>Đội ngũ Shoozy Shop</strong></p>
                    </div>
                    <div class="footer">
                        <p>📧 Email: shopshoozy@gmail.com | 📞 Hotline: 1900-xxxx</p>
                        <p>🌐 Website: shoozy-shop.com</p>
                    </div>
                </div>
            </body>
            </html>
            """, fullName, message);
    }
}