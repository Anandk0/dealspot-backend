package com.dealspot.config;

import com.dealspot.entity.PlatformSetting;
import com.dealspot.entity.User;
import com.dealspot.repository.PlatformSettingRepository;
import com.dealspot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Seeds initial data on first application run.
 * Creates the SUPER_ADMIN user and default platform settings if they don't already exist.
 * All operations are idempotent — safe to run on every startup.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PlatformSettingRepository platformSettingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.phone:+919999999999}")
    private String adminPhone;

    @Value("${admin.seed.password:admin123}")
    private String adminPassword;

    @Value("${admin.seed.name:Super Admin}")
    private String adminName;

    @Override
    public void run(String... args) {
        seedSuperAdmin();
        seedPlatformSettings();
    }

    private void seedSuperAdmin() {
        if (userRepository.existsByRole("SUPER_ADMIN")) {
            log.info("SUPER_ADMIN user already exists, skipping seed.");
            return;
        }

        User superAdmin = User.builder()
                .phone(adminPhone)
                .password(passwordEncoder.encode(adminPassword))
                .name(adminName)
                .role("SUPER_ADMIN")
                .phoneVerified(true)
                .banned(false)
                .verified(true)
                .verifiedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(superAdmin);
        log.info("Seeded SUPER_ADMIN user with phone: {}", adminPhone);
    }

    private void seedPlatformSettings() {
        Map<String, String> defaults = Map.of(
                "contact_unlock_price", "5000",
                "max_images_per_listing", "5",
                "listing_expiry_days", "30",
                "maintenance_mode", "false"
        );

        defaults.forEach((key, value) -> {
            if (!platformSettingRepository.existsById(key)) {
                PlatformSetting setting = PlatformSetting.builder()
                        .key(key)
                        .value(value)
                        .updatedAt(LocalDateTime.now())
                        .build();
                platformSettingRepository.save(setting);
                log.info("Seeded platform setting: {} = {}", key, value);
            }
        });
    }
}
