package com.eatzy.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.eatzy.auth.domain.EmailVerification;
import com.eatzy.auth.domain.User;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmailAndOtpCodeAndIsVerifiedFalse(String email, String otpCode);

    Optional<EmailVerification> findTopByUserOrderByCreatedAtDesc(User user);

    Optional<EmailVerification> findByUserAndIsVerifiedFalse(User user);

    Optional<EmailVerification> findTopByEmailAndIsVerifiedFalseOrderByCreatedAtDesc(String email);
}
