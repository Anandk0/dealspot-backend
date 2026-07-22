package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    /**
     * Role hierarchy: USER < CHECKER < ADMIN < SUPER_ADMIN
     * Index in the list determines the hierarchy level.
     */
    public static final List<String> ROLE_HIERARCHY = List.of("USER", "CHECKER", "ADMIN", "SUPER_ADMIN");

    /**
     * Checks if the user's role is in the set of allowed roles.
     * Throws AccessDeniedException if the user is null or their role is not in the allowed set.
     */
    public void checkRole(User user, String... allowedRoles) {
        if (user == null || !Arrays.asList(allowedRoles).contains(user.getRole())) {
            throw new AccessDeniedException("Insufficient permissions");
        }
    }

    public Page<User> getAllUsers(int page, int size, String search) {
        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.isBlank()) {
            return userRepository.findByNameContainingIgnoreCaseOrPhoneContaining(search, search, pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void banUser(Long userId, String reason, User actor) {
        checkRole(actor, "ADMIN", "SUPER_ADMIN");

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cannot ban users with equal/higher role (only SUPER_ADMIN can ban ADMIN)
        if (List.of("ADMIN", "SUPER_ADMIN").contains(target.getRole())
                && !"SUPER_ADMIN".equals(actor.getRole())) {
            throw new AccessDeniedException("Cannot ban admin users");
        }

        target.setBanned(true);
        target.setBanReason(reason);
        target.setBannedAt(LocalDateTime.now());
        userRepository.save(target);

        // Revoke all tokens
        refreshTokenRepository.revokeAllByUserId(userId);

        auditService.audit(actor, "BAN_USER", "USER", userId, reason);
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

        auditService.audit(actor, "UNBAN_USER", "USER", userId, null);
    }

    @Transactional
    public void changeUserRole(Long targetUserId, String newRole, User actor) {
        // Only SUPER_ADMIN can change roles
        checkRole(actor, "SUPER_ADMIN");

        // Cannot change own role
        if (actor.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot change own role");
        }

        // Validate target role - only USER, CHECKER, ADMIN are assignable
        List<String> validRoles = List.of("USER", "CHECKER", "ADMIN");
        if (!validRoles.contains(newRole)) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
        }

        // Cannot promote to SUPER_ADMIN (redundant with above but explicit for safety)
        if ("SUPER_ADMIN".equals(newRole)) {
            throw new IllegalArgumentException("Cannot assign SUPER_ADMIN role");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldRole = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);

        auditService.audit(actor, "CHANGE_ROLE", "USER", targetUserId,
                Map.of("oldRole", oldRole, "newRole", newRole).toString());
    }

    /**
     * Returns the hierarchy level of a role.
     * Higher value means higher privilege.
     */
    public static int getRoleLevel(String role) {
        int index = ROLE_HIERARCHY.indexOf(role);
        return index >= 0 ? index : -1;
    }
}
