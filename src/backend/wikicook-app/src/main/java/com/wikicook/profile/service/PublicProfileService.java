package com.wikicook.profile.service;

import com.wikicook.common.media.AvatarUrls;
import com.wikicook.profile.dto.PublicProfileDto;
import com.wikicook.profile.dto.PublicRecipeCardDto;
import com.wikicook.profile.dto.PublicRecipePageDto;
import com.wikicook.profile.port.PublicRecipesPort;
import com.wikicook.profile.repository.UserProfileRepository;
import com.wikicook.profile.repository.UserProfileRepository.PublicRow;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The public page of a person, for everybody including guests (US2-6, FR-005, research R3 level 1). It reads only the
 * public columns ({@link PublicRow}), so a private field can not leak by being forgotten in a mapper. A person who does
 * not exist, was deleted, or is {@code BANNED} is the same {@code USER_NOT_FOUND}: the answer does not tell them apart
 * (OQ-011-02, OQ-011-03; research.md row 11, not approved yet). A {@code SUSPENDED} person can still be seen.
 */
@Service
public class PublicProfileService {

    private final UserProfileRepository profiles;
    private final PublicRecipesPort recipes;

    public PublicProfileService(UserProfileRepository profiles, PublicRecipesPort recipes) {
        this.profiles = profiles;
        this.recipes = recipes;
    }

    public PublicProfileDto get(long userId) {
        PublicRow row = visible(userId);
        return new PublicProfileDto(row.getId(), row.getDisplayName(), AvatarUrls.of(row.getAvatarStorageKey()), row.getBio(),
                recipes.countPublishedByAuthor(userId));
    }

    public PublicRecipePageDto recipes(long userId, int page, int size) {
        visible(userId);
        List<PublicRecipeCardDto> items = recipes.findPublishedByAuthor(userId, page, size).stream()
                .map(card -> new PublicRecipeCardDto(card.id(), card.title(), card.coverImageUrl(), card.totalTimeMinutes(),
                        card.ratingAvg(), card.reviewCount()))
                .toList();
        return new PublicRecipePageDto(items, page, size, recipes.countPublishedByAuthor(userId));
    }

    private PublicRow visible(long userId) {
        return profiles.findPublic(userId).orElseThrow(() -> new ProfileException(ProfileErrorCode.USER_NOT_FOUND));
    }
}
