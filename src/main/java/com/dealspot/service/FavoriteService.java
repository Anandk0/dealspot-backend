package com.dealspot.service;

import com.dealspot.dto.ListingResponse;
import com.dealspot.entity.Favorite;
import com.dealspot.entity.Listing;
import com.dealspot.entity.User;
import com.dealspot.repository.FavoriteRepository;
import com.dealspot.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ListingRepository listingRepository;

    public void addFavorite(Long listingId, User user) {
        if (favoriteRepository.existsByUserIdAndListingId(user.getId(), listingId)) {
            throw new RuntimeException("Already in favorites");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        Favorite favorite = Favorite.builder()
                .user(user)
                .listing(listing)
                .build();

        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long listingId, User user) {
        favoriteRepository.deleteByUserIdAndListingId(user.getId(), listingId);
    }

    public Page<ListingResponse> getFavorites(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return favoriteRepository.findByUserId(userId, pageable)
                .map(fav -> ListingResponse.fromEntity(fav.getListing()));
    }

    public boolean isFavorite(Long listingId, Long userId) {
        return favoriteRepository.existsByUserIdAndListingId(userId, listingId);
    }
}
