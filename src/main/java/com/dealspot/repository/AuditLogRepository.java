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

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:from IS NULL OR a.createdAt >= :from) AND " +
            "(:to IS NULL OR a.createdAt <= :to) " +
            "ORDER BY a.createdAt DESC")
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
