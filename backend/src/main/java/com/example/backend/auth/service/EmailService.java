package com.example.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Zalo Clone] Xác thực tài khoản");
            helper.setText(buildVerificationHtml(firstName, code), true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendResetPasswordEmail(String toEmail, String firstName, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Zalo Clone] Đặt lại mật khẩu");
            helper.setText(buildResetPasswordHtml(firstName, code), true);

            mailSender.send(message);
            log.info("Reset password email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send reset password email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildVerificationHtml(String firstName, String code) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f9fafb;border-radius:16px;">
              <div style="text-align:center;margin-bottom:24px;">
                <div style="display:inline-block;background:#0068ff;border-radius:12px;padding:12px 20px;">
                  <span style="color:white;font-size:22px;font-weight:bold;">Zalo Clone</span>
                </div>
              </div>
              <div style="background:white;border-radius:12px;padding:32px;box-shadow:0 1px 4px rgba(0,0,0,0.06);">
                <h2 style="margin:0 0 8px;color:#1f2937;font-size:20px;">Xác thực tài khoản</h2>
                <p style="color:#6b7280;margin:0 0 24px;">Xin chào <strong>%s</strong>, vui lòng dùng mã OTP bên dưới để kích hoạt tài khoản.</p>
                <div style="background:#eff6ff;border-radius:12px;padding:20px;text-align:center;margin-bottom:24px;">
                  <span style="font-size:36px;font-weight:bold;letter-spacing:12px;color:#0068ff;">%s</span>
                </div>
                <p style="color:#9ca3af;font-size:13px;margin:0;">Mã có hiệu lực trong <strong>10 phút</strong>. Không chia sẻ mã này với bất kỳ ai.</p>
              </div>
            </div>
            """.formatted(firstName, code);
    }

    private String buildResetPasswordHtml(String firstName, String code) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f9fafb;border-radius:16px;">
              <div style="text-align:center;margin-bottom:24px;">
                <div style="display:inline-block;background:#0068ff;border-radius:12px;padding:12px 20px;">
                  <span style="color:white;font-size:22px;font-weight:bold;">Zalo Clone</span>
                </div>
              </div>
              <div style="background:white;border-radius:12px;padding:32px;box-shadow:0 1px 4px rgba(0,0,0,0.06);">
                <h2 style="margin:0 0 8px;color:#1f2937;font-size:20px;">Đặt lại mật khẩu</h2>
                <p style="color:#6b7280;margin:0 0 24px;">Xin chào <strong>%s</strong>, dùng mã OTP bên dưới để đặt lại mật khẩu của bạn.</p>
                <div style="background:#fff7ed;border-radius:12px;padding:20px;text-align:center;margin-bottom:24px;">
                  <span style="font-size:36px;font-weight:bold;letter-spacing:12px;color:#ea580c;">%s</span>
                </div>
                <p style="color:#9ca3af;font-size:13px;margin:0;">Mã có hiệu lực trong <strong>10 phút</strong>. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</p>
              </div>
            </div>
            """.formatted(firstName, code);
    }
}
