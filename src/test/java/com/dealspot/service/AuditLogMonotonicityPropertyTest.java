package com.dealspot.service;

import com.dealspot.entity.AuditLog;
import com.dealspot.entity.User;
import com.dealspot.repository.AuditLogRepository;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property 9: Audit log is append-only and monotonically growing.
 *
 * Generates random sequences of admin operations and verifies:
 * - Audit count never decreases (monotonically growing)
 * - Each mutation adds exactly one entry (save called once per audit call)
 * - No delete or update methods are ever called on the repository
 *
 * Validates: Requirements REQ-AUD-01, REQ-AUD-02, REQ-AUD-03, REQ-AUD-04, REQ-AUD-05
 */
@Tag("admin-panel")
@Tag("audit-log-monotonicity")
class AuditLogMonotonicityPropertyTest {

    private static final List<String> ACTIONS = List.of(
            "BAN_USER", "UNBAN_USER", "APPROVE_LISTING", "REJECT_LISTING",
            "CHANGE_ROLE", "UPDATE_SETTING", "CREATE_BANNER", "DELETE_BANNER"
    );

    private static final List<String> TARGET_TYPES = List.of(
            "USER", "LISTING", "BANNER", "SETTING"
    );

    /**
     * Property: For any random sequence of audit operations, the total save count
     * is monotonically increasing and each audit() call results in exactly one save().
     *
     * Validates: Requirements REQ-AUD-01, REQ-AUD-02, REQ-AUD-03, REQ-AUD-04, REQ-AUD-05
     */
    @Property(tries = 100)
    void auditLog_isAppendOnly_andMonotonicallyGrowing(
            @ForAll("auditOperationSequences") List<AuditOperation> operations
    ) {
        // Setup mock repository
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService auditService = new AuditService(repository);

        // Track save invocation count
        AtomicInteger saveCount = new AtomicInteger(0);
        when(repository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            saveCount.incrementAndGet();
            return invocation.getArgument(0);
        });

        User actor = User.builder()
                .id(1L)
                .phone("9000000001")
                .password("password123")
                .name("Admin User")
                .role("ADMIN")
                .banned(false)
                .build();

        int previousCount = 0;

        for (int i = 0; i < operations.size(); i++) {
            AuditOperation op = operations.get(i);

            // Execute audit call
            auditService.audit(actor, op.action(), op.targetType(), op.targetId(), op.details());

            int currentCount = saveCount.get();

            // Verify monotonicity: count never decreases
            assertTrue(currentCount >= previousCount,
                    "Audit count decreased from " + previousCount + " to " + currentCount +
                            " after operation " + (i + 1));

            // Verify each call adds exactly one entry
            assertEquals(i + 1, currentCount,
                    "Expected exactly " + (i + 1) + " save calls after " + (i + 1) +
                            " audit operations, but got " + currentCount);

            previousCount = currentCount;
        }

        // Verify no delete methods were ever called
        verify(repository, never()).deleteById(any());
        verify(repository, never()).delete(any(AuditLog.class));
        verify(repository, never()).deleteAll();
        verify(repository, never()).deleteAll(anyIterable());
        verify(repository, never()).deleteAllById(anyIterable());

        // Verify total save count equals total operations
        assertEquals(operations.size(), saveCount.get(),
                "Total save count should equal total number of audit operations");
    }

    /**
     * Property: Each audit() call captures the correct data in the saved entity.
     *
     * Validates: Requirements REQ-AUD-01, REQ-AUD-02, REQ-AUD-03, REQ-AUD-04
     */
    @Property(tries = 100)
    void auditLog_capturesCorrectData_forEachOperation(
            @ForAll("auditOperationSequences") List<AuditOperation> operations
    ) {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService auditService = new AuditService(repository);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        User actor = User.builder()
                .id(42L)
                .phone("9000000002")
                .password("password123")
                .name("Test Admin")
                .role("SUPER_ADMIN")
                .banned(false)
                .build();

        for (AuditOperation op : operations) {
            auditService.audit(actor, op.action(), op.targetType(), op.targetId(), op.details());
        }

        verify(repository, times(operations.size())).save(captor.capture());
        List<AuditLog> savedLogs = captor.getAllValues();

        assertEquals(operations.size(), savedLogs.size(),
                "Number of saved logs must match number of operations");

        for (int i = 0; i < operations.size(); i++) {
            AuditOperation op = operations.get(i);
            AuditLog log = savedLogs.get(i);

            assertEquals(42L, log.getActorId(), "Actor ID mismatch at operation " + i);
            assertEquals(op.action(), log.getAction(), "Action mismatch at operation " + i);
            assertEquals(op.targetType(), log.getTargetType(), "Target type mismatch at operation " + i);
            assertEquals(op.targetId(), log.getTargetId(), "Target ID mismatch at operation " + i);
            assertEquals(op.details(), log.getDetails(), "Details mismatch at operation " + i);
        }
    }

    // ─── Data Types ────────────────────────────────────────

    record AuditOperation(String action, String targetType, Long targetId, String details) {}

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<List<AuditOperation>> auditOperationSequences() {
        return auditOperations().list().ofMinSize(1).ofMaxSize(20);
    }

    private Arbitrary<AuditOperation> auditOperations() {
        Arbitrary<String> actions = Arbitraries.of(ACTIONS);
        Arbitrary<String> targetTypes = Arbitraries.of(TARGET_TYPES);
        Arbitrary<Long> targetIds = Arbitraries.longs().between(1L, 10000L);
        Arbitrary<String> details = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50);

        return Combinators.combine(actions, targetTypes, targetIds, details)
                .as(AuditOperation::new);
    }
}
