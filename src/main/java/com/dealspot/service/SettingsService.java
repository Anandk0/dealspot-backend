package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final PlatformSettingRepository settingRepository;
    private final AuditLogRepository auditLogRepository;

    public Map<String, String> getAllSettings() {
        Map<String, String> settings = new HashMap<>();
        settingRepository.findAll().forEach(s -> settings.put(s.getKey(), s.getValue()));
        return settings;
    }

    public void updateSetting(String key, String value, User actor) {
        PlatformSetting setting = settingRepository.findById(key)
                .orElse(PlatformSetting.builder().key(key).build());
        setting.setValue(value);
        setting.setUpdatedBy(actor.getId());
        setting.setUpdatedAt(LocalDateTime.now());
        settingRepository.save(setting);
        audit(actor, "UPDATE_SETTING", "SETTING", null, key + "=" + value);
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
