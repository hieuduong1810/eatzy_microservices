package com.eatzy.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.eatzy.auth.domain.EmailVerification;
import com.eatzy.auth.domain.User;
import com.eatzy.auth.repository.EmailVerificationRepository;
import com.eatzy.auth.repository.UserRepository;
import com.eatzy.common.exception.IdInvalidException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final Random random = new Random();

    public EmailVerificationService(
            EmailVerificationRepository emailVerificationRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendVerificationEmail(User user) throws IdInvalidException {
        if (user == null) {
            throw new IdInvalidException("User không được để trống");
        }

        if (user.getIsActive() != null && user.getIsActive()) {
            throw new IdInvalidException("Tài khoản đã được kích hoạt");
        }

        emailVerificationRepository.findByUserAndIsVerifiedFalse(user)
                .ifPresent(existing -> {
                    emailVerificationRepository.delete(existing);
                    log.info("Deleted existing unverified OTP for user: {}", user.getEmail());
                });

        String otpCode = generateOtp();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        EmailVerification verification = new EmailVerification(user, otpCode, expiresAt);
        emailVerificationRepository.save(verification);

        try {
            String baseUrl = getBaseUrl();
            emailService.sendVerificationEmail(
                    user.getEmail(),
                    user.getName() != null ? user.getName() : "User",
                    otpCode,
                    baseUrl);
            log.info("Verification OTP email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
            throw new IdInvalidException("Không thể gửi email xác thực. Vui lòng thử lại sau.");
        }
    }

    @Transactional
    public void resendVerificationEmail(String email) throws IdInvalidException {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new IdInvalidException("Không tìm thấy tài khoản với email: " + email);
        }

        if (user.getIsActive() != null && user.getIsActive()) {
            throw new IdInvalidException("Tài khoản đã được kích hoạt");
        }

        Optional<EmailVerification> existingOpt = emailVerificationRepository
                .findTopByEmailAndIsVerifiedFalseOrderByCreatedAtDesc(email);

        if (existingOpt.isPresent()) {
            EmailVerification existing = existingOpt.get();

            if (existing.getCreatedAt().isAfter(Instant.now().minus(1, ChronoUnit.MINUTES))) {
                throw new IdInvalidException(
                        "Vui lòng đợi ít nhất 1 phút trước khi gửi lại email xác thực");
            }
        }

        sendVerificationEmail(user);
    }

    @Transactional
    public User verifyEmail(String email, String otpCode) throws IdInvalidException {
        if (email == null || email.trim().isEmpty()) {
            throw new IdInvalidException("Email không được để trống");
        }

        if (otpCode == null || otpCode.trim().isEmpty()) {
            throw new IdInvalidException("Mã OTP không được để trống");
        }

        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findByEmailAndOtpCodeAndIsVerifiedFalse(email, otpCode);

        if (verificationOpt.isEmpty()) {
            throw new IdInvalidException("Mã OTP không hợp lệ hoặc đã được sử dụng");
        }

        EmailVerification verification = verificationOpt.get();

        if (verification.isExpired()) {
            throw new IdInvalidException(
                    "Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại mã mới");
        }

        verification.setIsVerified(true);
        verification.setVerifiedAt(Instant.now());
        emailVerificationRepository.save(verification);

        User user = verification.getUser();
        user.setIsActive(true);
        userRepository.save(user);

        try {
            String baseUrl = getBaseUrl();
            emailService.sendWelcomeEmail(
                    user.getEmail(),
                    user.getName() != null ? user.getName() : "User",
                    baseUrl);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to: {}", user.getEmail(), e);
        }

        log.info("Email verified successfully for user: {}", user.getEmail());

        return user;
    }

    public boolean isEmailVerified(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        User user = userRepository.findByEmail(email);

        if (user == null) {
            return false;
        }

        return user.getIsActive() != null && user.getIsActive();
    }

    public Optional<EmailVerification> getPendingVerification(User user) {
        return emailVerificationRepository.findByUserAndIsVerifiedFalse(user);
    }

    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private String getBaseUrl() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();

                StringBuilder baseUrl = new StringBuilder();
                baseUrl.append(scheme).append("://").append(serverName);

                if (("http".equals(scheme) && serverPort != 80) ||
                        ("https".equals(scheme) && serverPort != 443)) {
                    baseUrl.append(":").append(serverPort);
                }

                return baseUrl.toString();
            }
        } catch (Exception e) {
            log.warn("Could not extract base URL from request: {}", e.getMessage());
        }
        return "http://localhost:8080";
    }
}
