package com.eatzy.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eatzy.auth.domain.User;
import com.eatzy.auth.service.EmailVerificationService;
import com.eatzy.common.annotation.ApiMessage;
import com.eatzy.common.exception.IdInvalidException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@RestController
@RequestMapping("/api/v1/email")
@Tag(name = "Email Verification", description = "API xác thực email người dùng")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/verify-otp")
    @ApiMessage("Xác thực email qua OTP")
    @Operation(summary = "Xác thực email với OTP", description = "Xác thực email người dùng thông qua mã OTP 6 số được gửi qua email. "
            +
            "Sau khi xác thực thành công, tài khoản sẽ được kích hoạt (isActive = true).")
    public ResponseEntity<VerificationResponse> verifyEmailWithOtp(
            @Valid @RequestBody VerifyOtpRequest request)
            throws IdInvalidException {

        User user = emailVerificationService.verifyEmail(request.getEmail(), request.getOtpCode());

        VerificationResponse response = new VerificationResponse();
        response.setSuccess(true);
        response.setMessage("Xác thực email thành công! Tài khoản của bạn đã được kích hoạt.");
        response.setEmail(user.getEmail());
        response.setUserName(user.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend")
    @ApiMessage("Gửi lại email xác thực")
    @Operation(summary = "Gửi lại email xác thực", description = "Gửi lại email xác thực với mã OTP mới cho tài khoản chưa được kích hoạt. "
            +
            "Chỉ có thể gửi lại sau ít nhất 1 phút kể từ lần gửi trước.")
    public ResponseEntity<String> resendVerification(@Valid @RequestBody ResendRequest request)
            throws IdInvalidException {

        emailVerificationService.resendVerificationEmail(request.getEmail());

        return ResponseEntity.ok("Email xác thực với mã OTP mới đã được gửi. Vui lòng kiểm tra hộp thư của bạn.");
    }

    @GetMapping("/check")
    @ApiMessage("Kiểm tra trạng thái xác thực email")
    @Operation(summary = "Kiểm tra xác thực", description = "Kiểm tra xem email đã được xác thực hay chưa.")
    public ResponseEntity<CheckResponse> checkVerification(
            @RequestParam(name = "email", required = false) String email) {

        boolean isVerified = emailVerificationService.isEmailVerified(email);

        CheckResponse response = new CheckResponse();
        response.setEmail(email);
        response.setVerified(isVerified);
        response.setMessage(isVerified ? "Email đã được xác thực" : "Email chưa được xác thực");

        return ResponseEntity.ok(response);
    }

    @Getter
    @Setter
    public static class VerifyOtpRequest {
        @Email(message = "Email không hợp lệ")
        @NotBlank(message = "Email không được để trống")
        private String email;

        @NotBlank(message = "Mã OTP không được để trống")
        private String otpCode;
    }

    @Getter
    @Setter
    public static class ResendRequest {
        @Email(message = "Email không hợp lệ")
        @NotBlank(message = "Email không được để trống")
        private String email;
    }

    @Getter
    @Setter
    public static class VerificationResponse {
        private Boolean success;
        private String message;
        private String email;
        private String userName;
    }

    @Getter
    @Setter
    public static class CheckResponse {
        private String email;
        private Boolean verified;
        private String message;
    }
}
