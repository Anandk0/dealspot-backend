package com.dealspot.service;

import com.dealspot.entity.Report;
import com.dealspot.entity.User;
import com.dealspot.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

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
        return reportRepository.findByStatusOrderByCreatedAtDesc("PENDING", PageRequest.of(page, size));
    }
}
