package com.dealspot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${email-otp.enabled:false}")
    private boolean emailOtpEnabled;

    @Value("${email-otp.from:noreply@dealspot.in}")
    private String fromEmail;

    @Value("${email-otp.subject:Deal Spot - OTP Verification}")
    private String subject;

    public void sendOtpEmail(String toEmail, String otp) {
        if (!emailOtpEnabled) {
            log.info("EMAIL OTP DISABLED - OTP for {}: {}", toEmail, otp);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(
                "ನಮಸ್ಕಾರ!\n\n" +
                "ನಿಮ್ಮ Deal Spot OTP: " + otp + "\n\n" +
                "ಈ OTP 5 ನಿಮಿಷಗಳಲ್ಲಿ ಮುಕ್ತಾಯಗೊಳ್ಳುತ್ತದೆ.\n" +
                "ಯಾರೊಂದಿಗೂ ಹಂಚಿಕೊಳ್ಳಬೇಡಿ.\n\n" +
                "- Deal Spot Team"
            );
            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email");
        }
    }
}
