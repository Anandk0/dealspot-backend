package com.dealspot.service;

import com.dealspot.entity.OtpRecord;
import com.dealspot.repository.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpRepository otpRepository;
    @InjectMocks private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpEnabled", false);
        ReflectionTestUtils.setField(otpService, "expiryMinutes", 5);
    }

    @Test
    void sendOtp_inDevMode_shouldReturn123456() {
        when(otpRepository.save(any())).thenReturn(new OtpRecord());

        String otp = otpService.sendOtp("9876543210");

        assertEquals("123456", otp);
        verify(otpRepository).save(any(OtpRecord.class));
    }

    @Test
    void sendOtp_inProdMode_shouldReturnNull() {
        ReflectionTestUtils.setField(otpService, "otpEnabled", true);
        when(otpRepository.save(any())).thenReturn(new OtpRecord());

        String otp = otpService.sendOtp("9876543210");

        assertNull(otp); // OTP not returned in prod mode
    }

    @Test
    void verifyOtp_shouldSucceed_withCorrectCode() {
        OtpRecord record = OtpRecord.builder()
                .phone("9876543210")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        when(otpRepository.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(record));
        when(otpRepository.save(any())).thenReturn(record);

        assertTrue(otpService.verifyOtp("9876543210", "123456"));
        assertTrue(record.getVerified());
    }

    @Test
    void verifyOtp_shouldFail_withWrongCode() {
        OtpRecord record = OtpRecord.builder()
                .phone("9876543210")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        when(otpRepository.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(record));

        assertFalse(otpService.verifyOtp("9876543210", "000000"));
    }

    @Test
    void verifyOtp_shouldFail_whenExpired() {
        OtpRecord record = OtpRecord.builder()
                .phone("9876543210")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Expired
                .verified(false)
                .build();

        when(otpRepository.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(record));

        assertFalse(otpService.verifyOtp("9876543210", "123456"));
    }

    @Test
    void verifyOtp_shouldFail_whenNoRecord() {
        when(otpRepository.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc("0000000000"))
                .thenReturn(Optional.empty());

        assertFalse(otpService.verifyOtp("0000000000", "123456"));
    }
}
