package com.dealspot.repository;

import com.dealspot.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByActiveTrueOrderByCreatedAtDesc();

    // Eagerly fetch the creator so BannerResponse can expose creator id/name
    // without a LazyInitializationException outside the transaction.
    @Query("SELECT b FROM Banner b LEFT JOIN FETCH b.createdBy ORDER BY b.createdAt DESC")
    List<Banner> findAllWithCreator();
}
