package com.eatzy.communication.service;

import com.eatzy.common.email.Email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Transport layer for sending event-driven emails from communication-service.
 *
 * Responsibilities:
 *  - Owns its own JavaMailSender (independent from auth-service's).
 *  - Receives Email objects (created by EmailFactory from eatzy-common).
 *  - Sends async, event-driven notifications (restaurant approved, etc.).
 *
 * Why a separate JavaMailSender (not shared with auth-service):
 *  - Each service deploys independently — no runtime coupling.
 *  - communication-service's email is async/non-critical (fire-and-forget).
 *  - auth-service's email is sync/transactional (failure = rollback).
 */
@Service
@Slf4j
public class EmailSenderService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send an email using the provided Email DTO.
     * Non-blocking intent — callers (Kafka listeners) should handle failures gracefully.
     */
    public void send(Email email) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email.getTo());
            helper.setSubject(email.getSubject());
            helper.setText(email.getHtmlBody(), true);
            mailSender.send(message);
            log.info("📧 Email sent to: {} | Subject: {}", email.getTo(), email.getSubject());
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}: {}", email.getTo(), e.getMessage());
            // Do NOT rethrow — this is async/event-driven, failure should not crash the listener
        }
    }
}
