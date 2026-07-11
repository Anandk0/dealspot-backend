package com.dealspot.service;

import com.dealspot.entity.OtpRecord;
import com.dealspot.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;

    @Value("${otp.enabled}")
    private boolean otpEnabled;

    @Value("${otp.expiry-minutes}")
    private int expiryMinutes;

    private final Random random = new Random();

    /**
     * Generate and send OTP. When disabled, generates OTP "123456" for testing.
     * When enabled, integrate with SMS provider (MSG91, Twilio, etc.)
     */
    public String sendOtp(String phone) {
        String otp;

        if (!otpEnabled) {
            // Dev mode: always use 123456
            otp = "123456";
            log.info("DEV MODE OTP for {}: {}", phone, otp);
        } else {
            // Generate random 6-digit OTP
            otp = String.format("%06d", random.nextInt(999999));
            // TODO: Send via SMS provider
            // msg91Service.sendOtp(phone, otp);
            // OR twilioService.sendOtp(phone, otp);
            log.info("OTP sent to {}", phone);
        }

        OtpRecord record = OtpRecord.builder()
                .phone(phone)
                .otpCode(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();

        otpRepository.save(record);
        return otpEnabled ? null : otp; // Return OTP only in dev mode for testing
    }

    /**
     * Verify OTP entered by user.
     */
    public boolean verifyOtp(String phone, String otpCode) {
        OtpRecord record = otpRepository
                .findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(phone)
                .orElse(null);

        if (record == null) return false;
        if (record.isExpired()) return false;
        if (!record.getOtpCode().equals(otpCode)) return false;

        record.setVerified(true);
        otpRepository.save(record);
        return true;
    }
}
