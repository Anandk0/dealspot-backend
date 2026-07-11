package com.dealspot.repository;

import com.dealspot.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpRecord, Long> {
    Optional<OtpRecord> findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(String phone);
}
