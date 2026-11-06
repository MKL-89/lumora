package com.lumora.sms.security.reset;

import com.lumora.sms.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Delivers a one-time reset URL over the configured SMTP provider.
 *
 * <p>When no mail server is configured the link is written to the application log
 * instead. A reset must never fail because mail is unavailable, and the token is
 * still single-use and short-lived either way.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetMailService {

    private final JavaMailSender sender;

    @Value("${app.security.reset.base-url}")
    private String baseUrl;

    @Value("${app.security.reset.mail-enabled:false}")
    private boolean mailEnabled;

    public void send(User user, String token) {
        String link = baseUrl + "/password-reset/confirm?token=" + token;

        if (!mailEnabled) {
            log.info("Password reset link for {} (mail delivery disabled): {}", user.getUsername(), link);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Lumora password reset");
            message.setText("Use this one-time link within 30 minutes: " + link);
            sender.send(message);
        } catch (MailException ex) {
            // Log the failure, not the token, and let the reset stand.
            log.warn("Could not email the reset link to {}: {}", user.getUsername(), ex.getMessage());
        }
    }
}
