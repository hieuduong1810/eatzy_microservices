package com.eatzy.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String userName, String otpCode, String baseUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản Food Delivery");

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <style>
                                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                                    .otp-box { background: white; border: 2px dashed #667eea; border-radius: 10px; padding: 20px; margin: 30px 0; text-align: center; }
                                    .otp-code { font-size: 36px; font-weight: bold; letter-spacing: 10px; color: #667eea; font-family: 'Courier New', monospace; }
                                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                                    .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h1>🍔 Food Delivery</h1>
                                        <p>Xác thực tài khoản của bạn</p>
                                    </div>
                                    <div class="content">
                                        <h2>Xin chào %s!</h2>
                                        <p>Cảm ơn bạn đã đăng ký tài khoản tại Food Delivery.</p>
                                        <p>Để hoàn tất quá trình đăng ký và kích hoạt tài khoản, vui lòng nhập mã OTP sau vào trang xác thực:</p>

                                        <div class="otp-box">
                                            <p style="margin: 0; font-size: 14px; color: #666;">Mã xác thực của bạn</p>
                                            <div class="otp-code">%s</div>
                                        </div>

                                        <div class="warning">
                                            <strong>⏰ Lưu ý:</strong> Mã OTP này có hiệu lực trong <strong>15 phút</strong>.
                                        </div>

                                        <p>Nếu bạn không tạo tài khoản này, vui lòng bỏ qua email này.</p>

                                        <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">

                                        <p><strong>Tại sao bạn nhận được email này?</strong></p>
                                        <p>Email này được gửi đến <strong>%s</strong> vì địa chỉ email này được sử dụng để đăng ký tài khoản Food Delivery.</p>
                                    </div>
                                    <div class="footer">
                                        <p>© 2025 Food Delivery. All rights reserved.</p>
                                        <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    userName, otpCode, toEmail);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification OTP email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String userName, String baseUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Chào mừng bạn đến với Food Delivery!");

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <style>
                                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                                    .feature { display: inline-block; width: 30%%; text-align: center; margin: 10px; }
                                    .feature-icon { font-size: 40px; }
                                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h1>🎉 Chào mừng!</h1>
                                    </div>
                                    <div class="content">
                                        <h2>Xin chào %s!</h2>
                                        <p>Tài khoản của bạn đã được xác thực thành công! 🎊</p>
                                        <p>Bây giờ bạn có thể sử dụng đầy đủ các tính năng của Food Delivery:</p>

                                        <div style="text-align: center; margin: 30px 0;">
                                            <div class="feature">
                                                <div class="feature-icon">🍕</div>
                                                <p>Đặt món từ hàng trăm nhà hàng</p>
                                            </div>
                                            <div class="feature">
                                                <div class="feature-icon">⚡</div>
                                                <p>Giao hàng nhanh chóng</p>
                                            </div>
                                            <div class="feature">
                                                <div class="feature-icon">💳</div>
                                                <p>Thanh toán tiện lợi</p>
                                            </div>
                                        </div>

                                        <p>Chúc bạn có trải nghiệm tuyệt vời với dịch vụ của chúng tôi!</p>
                                    </div>
                                    <div class="footer">
                                        <p>© 2025 Food Delivery. All rights reserved.</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    userName);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }
}
