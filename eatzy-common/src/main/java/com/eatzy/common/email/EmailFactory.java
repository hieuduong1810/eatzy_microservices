package com.eatzy.common.email;

/**
 * Factory Pattern: Centralized factory for creating all email content.
 *
 * Purpose: Eliminate DRY violation — all HTML email templates are defined
 * ONCE here and reused by any service that needs to send email.
 *
 * Usage:
 *   Email email = EmailFactory.createOtpEmail(to, userName, otp);
 *   emailSenderService.send(email);
 *
 * Each service has its own JavaMailSender (no coupling), but shares
 * the same templates via this factory from eatzy-common.
 */
public class EmailFactory {

    private EmailFactory() {
        // Static utility class — do not instantiate
    }

    // =====================================================================
    // OTP / Verification Email
    // =====================================================================

    /**
     * Creates an OTP verification email.
     *
     * @param to       Recipient email address
     * @param userName Recipient's display name
     * @param otpCode  6-digit OTP code
     * @return Email object ready to be sent
     */
    public static Email createOtpEmail(String to, String userName, String otpCode) {
        String htmlBody = String.format("""
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
                            <h1>🍔 Eatzy</h1>
                            <p>Xác thực tài khoản của bạn</p>
                        </div>
                        <div class="content">
                            <h2>Xin chào %s!</h2>
                            <p>Cảm ơn bạn đã đăng ký tài khoản tại Eatzy.</p>
                            <p>Để hoàn tất quá trình đăng ký, vui lòng nhập mã OTP sau:</p>
                            <div class="otp-box">
                                <p style="margin: 0; font-size: 14px; color: #666;">Mã xác thực của bạn</p>
                                <div class="otp-code">%s</div>
                            </div>
                            <div class="warning">
                                <strong>⏰ Lưu ý:</strong> Mã OTP này có hiệu lực trong <strong>15 phút</strong>.
                            </div>
                            <p>Nếu bạn không tạo tài khoản này, vui lòng bỏ qua email này.</p>
                            <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">
                            <p>Email này được gửi đến <strong>%s</strong> vì địa chỉ email này được dùng để đăng ký Eatzy.</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 Eatzy. All rights reserved.</p>
                            <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, userName, otpCode, to);

        return Email.builder()
                .to(to)
                .subject("Xác thực tài khoản Eatzy")
                .htmlBody(htmlBody)
                .build();
    }

    // =====================================================================
    // Welcome Email
    // =====================================================================

    /**
     * Creates a welcome email sent after successful email verification.
     *
     * @param to       Recipient email address
     * @param userName Recipient's display name
     * @return Email object ready to be sent
     */
    public static Email createWelcomeEmail(String to, String userName) {
        String htmlBody = String.format("""
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
                            <p>Bây giờ bạn có thể sử dụng đầy đủ các tính năng của Eatzy:</p>
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
                            <p>© 2025 Eatzy. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, userName);

        return Email.builder()
                .to(to)
                .subject("Chào mừng bạn đến với Eatzy! 🎉")
                .htmlBody(htmlBody)
                .build();
    }

    // =====================================================================
    // Restaurant Approved Email
    // =====================================================================

    /**
     * Creates a notification email sent to a restaurant owner when their
     * restaurant is approved by admin.
     *
     * @param to             Owner's email address
     * @param restaurantName Name of the approved restaurant
     * @return Email object ready to be sent
     */
    public static Email createRestaurantApprovedEmail(String to, String restaurantName) {
        String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .badge { background: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; margin: 20px 0; border-radius: 0 8px 8px 0; }
                        .cta { text-align: center; margin: 30px 0; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎊 Chúc mừng!</h1>
                            <p>Nhà hàng của bạn đã được duyệt</p>
                        </div>
                        <div class="content">
                            <div class="badge">
                                <strong>✅ Nhà hàng "%s" đã được phê duyệt thành công!</strong>
                            </div>
                            <p>Xin chào,</p>
                            <p>Chúng tôi rất vui mừng thông báo rằng nhà hàng <strong>%s</strong>
                            của bạn đã được đội ngũ Eatzy xem xét và phê duyệt.</p>
                            <p>Từ bây giờ, nhà hàng của bạn sẽ được hiển thị trên nền tảng Eatzy
                            và bạn có thể bắt đầu nhận đơn hàng từ khách hàng ngay lập tức.</p>
                            <div class="cta">
                                <p>🍽️ Hãy đăng nhập vào hệ thống và bắt đầu quản lý menu của bạn!</p>
                            </div>
                            <p>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ đội ngũ hỗ trợ của chúng tôi.</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 Eatzy. All rights reserved.</p>
                            <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, restaurantName, restaurantName);

        return Email.builder()
                .to(to)
                .subject("🎊 Nhà hàng " + restaurantName + " đã được duyệt trên Eatzy!")
                .htmlBody(htmlBody)
                .build();
    }
}
