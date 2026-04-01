package com.eatzy.auth.domain;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@NoArgsConstructor
public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String email;
    private String otpCode;
    private Boolean isVerified;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant verifiedAt;

    public EmailVerification(User user, String otpCode, Instant expiresAt) {
        this.user = user;
        this.email = user.getEmail();
        this.otpCode = otpCode;
        this.expiresAt = expiresAt;
        this.isVerified = false;
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
