package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;

    public void checkRole(User user, String... allowedRoles) {
        for (String role : allowedRoles) {
            if (user.getRole().equals(role)) return;
        }
        throw new RuntimeException("Access denied. Required role: " + String.join(" or ", allowedRoles));
    }

    public Page<User> getAllUsers(int page, int size, String search) {
        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.isBlank()) {
            return userRepository.findAll(pageable); // TODO: add search query
        }
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void banUser(Long userId, String reason, User actor) {
        checkRole(actor, "ADMIN", "SUPER_ADMIN");

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        target.setBanned(true);
        target.setBanReason(reason);
        target.setBannedAt(LocalDateTime.now());
        userRepository.save(target);

        // Revoke all tokens
        refreshTokenRepository.revokeAllByUserId(userId);

        audit(actor, "BAN_USER", "USER", userId, reason);
    }

    @Transactional
    public void unbanUser(Long userId, User actor) {
        checkRole(actor, "ADMIN", "SUPER_ADMIN");

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        target.setBanned(false);
        target.setBanReason(null);
        target.setBannedAt(null);
        userRepository.save(target);

        audit(actor, "UNBAN_USER", "USER", userId, null);
    }

    @Transactional
    public void changeUserRole(Long targetUserId, String newRole, User actor) {
        checkRole(actor, "SUPER_ADMIN");

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldRole = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);

        audit(actor, "ROLE_CHANGE", "USER", targetUserId, oldRole + " → " + newRole);
    }

    private void audit(User actor, String action, String targetType, Long targetId, String details) {
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
