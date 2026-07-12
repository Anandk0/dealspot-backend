package com.dealspot.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dealspot.entity.Media;
import com.dealspot.entity.User;
import com.dealspot.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final MediaRepository mediaRepository;

    /**
     * Upload image and save media record
     */
    public Media uploadImage(MultipartFile file, String entityType, Long entityId, User uploader) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                        "folder", "dealspot/" + entityType.toLowerCase(),
                        "resource_type", "image"
                    ));

            Media media = Media.builder()
                    .cloudinaryPublicId((String) result.get("public_id"))
                    .cloudinaryUrl((String) result.get("url"))
                    .secureUrl((String) result.get("secure_url"))
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .width(result.get("width") != null ? ((Number) result.get("width")).intValue() : null)
                    .height(result.get("height") != null ? ((Number) result.get("height")).intValue() : null)
                    .format((String) result.get("format"))
                    .resourceType("image")
                    .folder("dealspot/" + entityType.toLowerCase())
                    .entityType(entityType)
                    .entityId(entityId)
                    .uploadedBy(uploader)
                    .build();

            return mediaRepository.save(media);
        } catch (IOException e) {
            log.error("Failed to upload image: {}", e.getMessage());
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Simple upload that returns just the URL (backward compatible)
     */
    public String uploadImageUrl(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                        "folder", "dealspot",
                        "resource_type", "image"
                    ));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Delete image from Cloudinary and remove media record
     */
    public void deleteImage(Long mediaId) {
        Media media = mediaRepository.findById(mediaId).orElse(null);
        if (media == null) return;

        try {
            cloudinary.uploader().destroy(media.getCloudinaryPublicId(), ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Failed to delete from Cloudinary: {}", e.getMessage());
        }

        mediaRepository.delete(media);
    }

    /**
     * Delete image by public ID
     */
    public void deleteByPublicId(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            mediaRepository.findByCloudinaryPublicId(publicId).ifPresent(mediaRepository::delete);
        } catch (IOException e) {
            log.warn("Failed to delete from Cloudinary: {}", e.getMessage());
        }
    }

    /**
     * Get all media for an entity
     */
    public List<Media> getMediaForEntity(String entityType, Long entityId) {
        return mediaRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}
