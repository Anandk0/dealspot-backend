package com.dealspot.repository;

import com.dealspot.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * APPEND-ONLY CONTRACT: This repository must only be used for inserts (save) and reads (find).
 * No code in the application should call delete*, removeAll, or any update on existing audit log entries.
 * Audit logs are immutable once written (REQ-AUD-05).
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLog> findByActionAndCreatedAtBetweenOrderByCreatedAtDesc(String action, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // Native query with explicit casts. PostgreSQL cannot infer the type of a bare
    // parameter used only in "? IS NULL", so we cast each nullable filter param to
    // its concrete type (text / timestamp). A separate countQuery is required for
    // pagination since native queries can't derive it automatically.
    @Query(value = "SELECT * FROM admin_audit_log a WHERE " +
            "(CAST(:action AS text) IS NULL OR a.action = :action) AND " +
            "(CAST(:from AS timestamp) IS NULL OR a.created_at >= :from) AND " +
            "(CAST(:to AS timestamp) IS NULL OR a.created_at <= :to) " +
            "ORDER BY a.created_at DESC",
            countQuery = "SELECT count(*) FROM admin_audit_log a WHERE " +
            "(CAST(:action AS text) IS NULL OR a.action = :action) AND " +
            "(CAST(:from AS timestamp) IS NULL OR a.created_at >= :from) AND " +
            "(CAST(:to AS timestamp) IS NULL OR a.created_at <= :to)",
            nativeQuery = true)
    Page<AuditLog> findFiltered(
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    /**
     * @deprecated DO NOT USE - Audit logs are append-only. This method is inherited from JpaRepository
     * but must never be called. Calling it violates REQ-AUD-05.
     */
    @Deprecated
    @Override
    void deleteById(Long id);

    /**
     * @deprecated DO NOT USE - Audit logs are append-only. This method is inherited from JpaRepository
     * but must never be called. Calling it violates REQ-AUD-05.
     */
    @Deprecated
    @Override
    void delete(AuditLog entity);

    /**
     * @deprecated DO NOT USE - Audit logs are append-only. This method is inherited from JpaRepository
     * but must never be called. Calling it violates REQ-AUD-05.
     */
    @Deprecated
    @Override
    void deleteAll();

    /**
     * @deprecated DO NOT USE - Audit logs are append-only. This method is inherited from JpaRepository
     * but must never be called. Calling it violates REQ-AUD-05.
     */
    @Deprecated
    @Override
    void deleteAll(Iterable<? extends AuditLog> entities);

    /**
     * @deprecated DO NOT USE - Audit logs are append-only. This method is inherited from JpaRepository
     * but must never be called. Calling it violates REQ-AUD-05.
     */
    @Deprecated
    @Override
    void deleteAllById(Iterable<? extends Long> ids);
}
