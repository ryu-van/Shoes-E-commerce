package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.CouponSendRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private SpringTemplateEngine templateEngine;

    @Value("app.mail.from")
    private String from;

    @Value("app.mail.sender-name")
    private String senderName;

    public void sendCouponToCustomer(CouponSendRequest req){
        if (req.getTo() == null || req.getTo().isEmpty()) {
            throw new IllegalArgumentException("'to' (người nhận) là bắt buộc");
        }

        String html = renderCoupnonTemplate(req);

        sendHtml(req.getTo(),req.getSubject(),html);



    }
    private  String renderCoupnonTemplate(CouponSendRequest req){
        Context ctx = new Context(new Locale("vi", "VN"));
        ctx.setVariable("codeCoupon", req.getCodeCoupon());
        ctx.setVariable("typeCoupon", req.getTypeCoupon());
        ctx.setVariable("valueCoupon", req.getValueCoupon());
        ctx.setVariable("valueLimit", req.getValueLimit());
        ctx.setVariable("startDate",fmtDate(req.getStartDate()));
        ctx.setVariable("endDate", fmtDate(req.getEndDate()));
        ctx.setVariable("condition", req.getCondition());
        ctx.setVariable("homeUrl", req.getHomeUrl());
        ctx.setVariable("year", String.valueOf(java.time.Year.now()));
        return templateEngine.process("coupon-template", ctx);
    }
    private void sendHtml(List<String> to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");

            helper.setFrom(from, senderName);
            helper.setTo(to.toArray(String[]::new));
            helper.setSubject(Objects.requireNonNullElse(subject, "Ưu đãi từ Shoozy Shop"));
            helper.setText(html, true);

            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Send mail failed", e);
        }
    }

    private String fmtDate(String raw) {
        if (raw == null || raw.isBlank()) return "";
        raw = raw.trim();

        String[] dPat = {"yyyy-MM-dd", "dd/MM/yyyy", "yyyy/MM/dd"};
        for (String p : dPat) {
            try {
                return LocalDate.parse(raw, DateTimeFormatter.ofPattern(p))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignored) {}
        }
        String[] dtPat = {"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss"};
        for (String p : dtPat) {
            try {
                return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern(p))
                        .toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignored) {}
        }
        return raw;
    }
}
