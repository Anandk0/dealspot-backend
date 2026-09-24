package com.dealspot.repository;

import com.dealspot.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpRecord, Long> {
    Optional<OtpRecord> findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(String phone);

    // Latest unverified OTP for an email
    Optional<OtpRecord> findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc(String email);

    // Latest VERIFIED OTP for an email (used to confirm registration is allowed)
    Optional<OtpRecord> findTopByEmailAndVerifiedTrueOrderByCreatedAtDesc(String email);
}
