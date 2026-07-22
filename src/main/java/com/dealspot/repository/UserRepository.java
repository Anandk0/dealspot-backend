package com.dealspot.repository;

import com.dealspot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByRole(String role);

    Page<User> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone, Pageable pageable);

    // Daily registrations: returns [date, count] pairs for users created within date range
    @Query("SELECT CAST(u.createdAt AS LocalDate), COUNT(u) " +
           "FROM User u " +
           "WHERE u.createdAt BETWEEN :from AND :to " +
           "GROUP BY CAST(u.createdAt AS LocalDate) " +
           "ORDER BY CAST(u.createdAt AS LocalDate) ASC")
    List<Object[]> findDailyRegistrations(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
