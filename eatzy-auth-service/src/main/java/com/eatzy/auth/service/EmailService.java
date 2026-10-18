package com.eatzy.auth.service;

import com.eatzy.common.email.Email;
import com.eatzy.common.email.EmailFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Transport layer for sending emails from auth-service.
 *
 * Responsibilities:
 *  - Owns JavaMailSender (SMTP connection for auth-service).
 *  - Delegates email CONTENT creation to EmailFactory (from eatzy-common).
 *  - Handles transactional emails: OTP verification, Welcome.
 *
 * Why emails stay here (not in communication-service):
 *  - OTP email is transactional — must succeed synchronously.
 *  - If sending fails, the caller throws an exception to rollback the flow.
 *  - Making it async (via Kafka) would break this guarantee.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send OTP verification email.
     * Called synchronously during user registration — failure causes rollback.
     */
    public void sendVerificationEmail(String toEmail, String userName, String otpCode, String baseUrl) {
        Email email = EmailFactory.createOtpEmail(toEmail, userName, otpCode);
        send(email);
        log.info("Verification OTP email sent to: {}", toEmail);
    }

    /**
     * Send welcome email after successful email verification.
     * Non-critical — failure is logged but does not block the verification flow.
     */
    public void sendWelcomeEmail(String toEmail, String userName, String baseUrl) {
        Email email = EmailFactory.createWelcomeEmail(toEmail, userName);
        try {
            send(email);
            log.info("Welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
            // Non-critical: do not rethrow — verification is already complete
        }
    }

    /**
     * Core send method — converts Email DTO to MimeMessage and sends via SMTP.
     */
    private void send(Email email) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email.getTo());
            helper.setSubject(email.getSubject());
            helper.setText(email.getHtmlBody(), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", email.getTo(), e.getMessage());
            throw new RuntimeException("Failed to send email to " + email.getTo(), e);
        }
    }
}
