package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final AuditLogRepository auditLogRepository;

    public List<Banner> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<Banner> getAllBanners() {
        return bannerRepository.findAll(Sort.by("createdAt").descending());
    }

    public Banner createBanner(Banner banner, User actor) {
        banner.setCreatedBy(actor);
        Banner saved = bannerRepository.save(banner);
        audit(actor, "CREATE_BANNER", "BANNER", saved.getId(), banner.getTitle());
        return saved;
    }

    public void deleteBanner(Long bannerId, User actor) {
        bannerRepository.deleteById(bannerId);
        audit(actor, "DELETE_BANNER", "BANNER", bannerId, null);
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
