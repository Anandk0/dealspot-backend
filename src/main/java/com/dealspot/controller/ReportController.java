package com.dealspot.controller;

import com.dealspot.dto.CreateReportRequest;
import com.dealspot.dto.ReportResponse;
import com.dealspot.entity.User;
import com.dealspot.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ReportResponse.fromEntity(
                reportService.createReport(
                        request.getTargetType(),
                        request.getTargetId(),
                        request.getReason(),
                        request.getDescription(),
                        user)));
    }
}
