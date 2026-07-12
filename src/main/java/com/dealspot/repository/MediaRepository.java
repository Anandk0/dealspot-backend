package com.dealspot.repository;

import com.dealspot.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByEntityTypeAndEntityId(String entityType, Long entityId);
    Optional<Media> findByCloudinaryPublicId(String publicId);
    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);
}
