package com.dealspot.service;

import com.dealspot.entity.AuditLog;
import com.dealspot.entity.User;
import com.dealspot.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private User superAdmin;

    @BeforeEach
    void setUp() {
        superAdmin = User.builder().id(1L).phone("9000000001").name("Super").role("SUPER_ADMIN").banned(false).build();
    }

    // ─── Audit Logging ────────────────────────────────────

    @Test
    void audit_shouldCreateAppendOnlyLogEntry() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.audit(superAdmin, "BAN_USER", "USER", 5L, "Spamming");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(1L, saved.getActorId());
        assertEquals("BAN_USER", saved.getAction());
        assertEquals("USER", saved.getTargetType());
        assertEquals(5L, saved.getTargetId());
        assertEquals("Spamming", saved.getDetails());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void audit_shouldAllowNullDetails() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.audit(superAdmin, "APPROVE_LISTING", "LISTING", 10L, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("APPROVE_LISTING", saved.getAction());
        assertNull(saved.getDetails());
    }

    @Test
    void audit_shouldAllowNullTargetId() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.audit(superAdmin, "UPDATE_SETTING", "SETTING", null, "contact_unlock_price=6000");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("UPDATE_SETTING", saved.getAction());
        assertNull(saved.getTargetId());
        assertEquals("contact_unlock_price=6000", saved.getDetails());
    }

    // ─── Filtering ────────────────────────────────────────

    @Test
    void getAuditLogs_withNoFilters_shouldCallFindFilteredWithNulls() {
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findFiltered(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<AuditLog> result = auditService.getAuditLogs(0, 20);

        assertNotNull(result);
        verify(auditLogRepository).findFiltered(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAuditLogs_withActionFilter_shouldPassActionToRepository() {
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findFiltered(eq("BAN_USER"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<AuditLog> result = auditService.getAuditLogs(0, 20, "BAN_USER", null, null);

        assertNotNull(result);
        verify(auditLogRepository).findFiltered(eq("BAN_USER"), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAuditLogs_withDateRange_shouldConvertDatesToDateTimes() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        LocalDateTime expectedFrom = from.atStartOfDay();
        LocalDateTime expectedTo = to.atTime(23, 59, 59);

        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findFiltered(isNull(), eq(expectedFrom), eq(expectedTo), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<AuditLog> result = auditService.getAuditLogs(0, 50, null, from, to);

        assertNotNull(result);
        verify(auditLogRepository).findFiltered(isNull(), eq(expectedFrom), eq(expectedTo), any(Pageable.class));
    }

    @Test
    void getAuditLogs_withAllFilters_shouldPassAllToRepository() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        LocalDateTime expectedFrom = from.atStartOfDay();
        LocalDateTime expectedTo = to.atTime(23, 59, 59);

        AuditLog log = AuditLog.builder()
                .id(1L)
                .actorId(1L)
                .action("CHANGE_ROLE")
                .targetType("USER")
                .targetId(5L)
                .details("{oldRole=USER, newRole=ADMIN}")
                .createdAt(LocalDateTime.of(2026, 6, 15, 10, 0, 0))
                .build();
        Page<AuditLog> page = new PageImpl<>(List.of(log));

        when(auditLogRepository.findFiltered(eq("CHANGE_ROLE"), eq(expectedFrom), eq(expectedTo), any(Pageable.class)))
                .thenReturn(page);

        Page<AuditLog> result = auditService.getAuditLogs(0, 50, "CHANGE_ROLE", from, to);

        assertEquals(1, result.getTotalElements());
        assertEquals("CHANGE_ROLE", result.getContent().get(0).getAction());
    }

    @Test
    void getAuditLogs_withOnlyFromDate_shouldPassNullForTo() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDateTime expectedFrom = from.atStartOfDay();

        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findFiltered(isNull(), eq(expectedFrom), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<AuditLog> result = auditService.getAuditLogs(0, 20, null, from, null);

        assertNotNull(result);
        verify(auditLogRepository).findFiltered(isNull(), eq(expectedFrom), isNull(), any(Pageable.class));
    }

    @Test
    void getAuditLogs_withOnlyToDate_shouldPassNullForFrom() {
        LocalDate to = LocalDate.of(2026, 7, 31);
        LocalDateTime expectedTo = to.atTime(23, 59, 59);

        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findFiltered(isNull(), isNull(), eq(expectedTo), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<AuditLog> result = auditService.getAuditLogs(0, 20, null, null, to);

        assertNotNull(result);
        verify(auditLogRepository).findFiltered(isNull(), isNull(), eq(expectedTo), any(Pageable.class));
    }
}
