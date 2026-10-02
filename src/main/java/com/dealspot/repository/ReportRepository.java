package com.dealspot.repository;

import com.dealspot.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, String targetType, Long targetId);

    // All reports (any status) against a set of listing ids — used to show a
    // moderator which of a seller's ads have been reported, and why.
    java.util.List<Report> findByTargetTypeAndTargetIdInOrderByCreatedAtDesc(
            String targetType, java.util.Collection<Long> targetIds);
}
