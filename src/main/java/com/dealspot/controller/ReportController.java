package com.dealspot.controller;

import com.dealspot.entity.Report;
import com.dealspot.entity.User;
import com.dealspot.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<Report> createReport(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.createReport(
                body.get("targetType"),
                Long.parseLong(body.get("targetId")),
                body.get("reason"),
                body.get("description"),
                user
        ));
    }
}
