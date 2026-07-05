package com.testgen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    // Base URL of the deployed frontend, e.g. https://your-app.up.railway.app
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = baseUrl + "/reset-password.html?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("TestLedger — Reset your password");
        message.setText(
                "We received a request to reset your TestLedger password.\n\n" +
                "Click the link below to choose a new password. This link expires in 30 minutes:\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email."
        );

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Don't leak SMTP errors to the caller — log it and let the
            // controller return a generic success message either way,
            // so we don't reveal whether an email address exists.
            log.error("Failed to send password reset email to {}", toEmail, e);
        }
    }
}
