package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Centralized audit logging service.
 * All admin mutations must use this service for audit trail recording.
 * The audit log is APPEND-ONLY — no updates or deletes are performed (REQ-AUD-05).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Retrieves audit logs with optional filtering by action type and date range.
     *
     * @param page   page number (0-indexed)
     * @param size   page size
     * @param action optional action type filter (e.g., "BAN_USER", "APPROVE_LISTING")
     * @param from   optional start date (inclusive)
     * @param to     optional end date (inclusive, end of day)
     * @return paginated audit logs ordered by createdAt descending
     */
    public Page<AuditLog> getAuditLogs(int page, int size, String action, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59) : null;

        return auditLogRepository.findFiltered(
                action,
                fromDateTime,
                toDateTime,
                PaginationUtil.createPageable(page, size));
    }

    /**
     * Overloaded method for backward compatibility — returns all logs without filters.
     */
    public Page<AuditLog> getAuditLogs(int page, int size) {
        return getAuditLogs(page, size, null, null, null);
    }

    /**
     * Appends an audit log entry. This is the ONLY way to write audit records.
     * No update or delete operations exist by design (REQ-AUD-05).
     *
     * @param actor      the user performing the action
     * @param action     the action type (e.g., "BAN_USER", "APPROVE_LISTING", "CHANGE_ROLE")
     * @param targetType the entity type being acted upon (e.g., "USER", "LISTING", "BANNER", "SETTING")
     * @param targetId   the ID of the target entity (nullable for settings)
     * @param details    additional context/details about the action
     */
    public void audit(User actor, String action, String targetType, Long targetId, String details) {
        AuditLog log = AuditLog.builder()
                .actorId(actor.getId())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }
}
