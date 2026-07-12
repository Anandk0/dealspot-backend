package com.dealspot.service;

import com.dealspot.entity.Report;
import com.dealspot.entity.User;
import com.dealspot.repository.ReportRepository;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    @Transactional
    public Report createReport(String targetType, Long targetId, String reason, String description, User reporter) {
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporter.getId(), targetType, targetId)) {
            throw new RuntimeException("You have already reported this");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .description(description)
                .build();

        return reportRepository.save(report);
    }

    public Page<Report> getPendingReports(int page, int size) {
        return reportRepository.findByStatusOrderByCreatedAtDesc("PENDING", PaginationUtil.createPageable(page, size));
    }
}
