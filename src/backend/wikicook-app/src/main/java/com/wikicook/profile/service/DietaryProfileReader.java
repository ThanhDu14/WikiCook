package com.wikicook.profile.service;

import com.wikicook.common.diet.DietaryProfileUnavailableException;
import com.wikicook.profile.repository.DietaryLinkRepository;
import com.wikicook.profile.repository.DietaryLinkRepository.Snapshot;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

/**
 * The one way to read a stored dietary profile for somebody else's use: the ports of 030, 031, 040 and 020 and
 * {@code GET /me/dietary-profile} all come through here. A failed read is {@link DietaryProfileUnavailableException},
 * never an empty profile (FR-026a); "no such user" is an empty {@link Optional}, which the ports answer with an empty
 * profile (FR-026). Nothing is cached, so the next call after a save sees the new values (FR-024, FR-025).
 */
@Service
public class DietaryProfileReader {

    private final DietaryLinkRepository links;

    public DietaryProfileReader(DietaryLinkRepository links) {
        this.links = links;
    }

    public Optional<Snapshot> read(long userId) {
        try {
            return links.findSnapshot(userId);
        } catch (DataAccessException | TransactionException e) {
            throw new DietaryProfileUnavailableException("Can not read the dietary profile of user " + userId, e);
        }
    }

    /** The ingredients 040 must keep out for these allergens ({@code ingredient_allergens}, FR-010). */
    public Set<Long> ingredientIdsWithAllergens(Collection<Long> allergenIds) {
        try {
            return links.ingredientIdsWithAllergens(allergenIds);
        } catch (DataAccessException | TransactionException e) {
            throw new DietaryProfileUnavailableException("Can not read the allergen ingredients", e);
        }
    }
}
