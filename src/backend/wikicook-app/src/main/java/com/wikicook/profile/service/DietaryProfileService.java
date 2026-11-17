package com.wikicook.profile.service;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.profile.domain.DietaryNotice;
import com.wikicook.profile.dto.AllergenDto;
import com.wikicook.profile.dto.CuisineDto;
import com.wikicook.profile.dto.DietaryNoticeDto;
import com.wikicook.profile.dto.DietaryOptionsDto;
import com.wikicook.profile.dto.DietaryProfileDto;
import com.wikicook.profile.dto.DietaryProfileUpdateRequest;
import com.wikicook.profile.dto.IngredientSuggestionDto;
import com.wikicook.profile.port.CuisineCatalogPort;
import com.wikicook.profile.port.IngredientCatalogPort;
import com.wikicook.profile.port.ProfileAuditPort;
import com.wikicook.profile.repository.DietaryLinkRepository;
import com.wikicook.profile.repository.DietaryLinkRepository.Snapshot;
import com.wikicook.profile.repository.DietaryNoticeRepository;
import com.wikicook.profile.service.ProfileException.FieldCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The dietary profile of the signed-in person (US1, research R5). The save replaces the whole form in one transaction:
 * what is wrong is refused before anything is written, identical content is a no-op, and the version check and the row
 * lock are one {@code UPDATE}. The service takes the person's id from the caller, never from the request, so it can
 * only ever touch the caller's own profile (FR-006, FR-018).
 */
@Service
public class DietaryProfileService {

    static final int MAX_EXCLUDED = 50;
    static final String AUDIT_ACTION = "DIETARY_PROFILE_UPDATED";

    private final DietaryLinkRepository links;
    private final DietaryProfileReader reader;
    private final IngredientCatalogPort ingredients;
    private final CuisineCatalogPort cuisines;
    private final DietaryNoticeRepository notices;
    private final ProfileAuditPort audit;

    public DietaryProfileService(DietaryLinkRepository links, DietaryProfileReader reader,
            IngredientCatalogPort ingredients, CuisineCatalogPort cuisines, DietaryNoticeRepository notices,
            ProfileAuditPort audit) {
        this.links = links;
        this.reader = reader;
        this.ingredients = ingredients;
        this.cuisines = cuisines;
        this.notices = notices;
        this.audit = audit;
    }

    /**
     * Not in a transaction on purpose: a database that is down is then reported by {@link DietaryProfileReader} as
     * {@code PROFILE_UNAVAILABLE}, not as a failure to open a transaction.
     */
    public DietaryProfileDto get(long userId) {
        Snapshot snapshot = reader.read(userId).orElseThrow(() -> new ProfileException(ProfileErrorCode.USER_NOT_FOUND));
        return toDto(userId, snapshot);
    }

    public DietaryOptionsDto options() {
        List<AllergenDto> allergens = links.listAllergens().stream()
                .map(row -> new AllergenDto(row.id(), row.code(), row.nameVi())).toList();
        List<CuisineDto> active = cuisines.listActive().stream().map(c -> new CuisineDto(c.id(), c.name())).toList();
        return new DietaryOptionsDto(allergens, List.of(DietType.values()), active);
    }

    @Transactional
    public DietaryProfileDto save(long userId, DietaryProfileUpdateRequest request) {
        Snapshot current = links.findSnapshot(userId).orElseThrow(() -> new ProfileException(ProfileErrorCode.USER_NOT_FOUND));
        validate(request, current);
        if (sameContent(current, request)) {
            return toDto(userId, current); // FR-041: nothing to write, nothing to audit, whatever the version
        }
        if (links.updateScalars(userId, request.version(), request.dietType(), request.householdSize(),
                request.maxCookTimeMinutes()) == 0) {
            throw new ProfileException(ProfileErrorCode.PROFILE_CHANGED);
        }
        links.replaceAllergens(userId, request.allergenIds());
        links.replaceExcludedIngredients(userId, request.excludedIngredientIds());
        links.replaceCuisines(userId, request.preferredCuisineIds());
        audit.record(userId, AUDIT_ACTION, Map.of()); // no details: even "an allergen was added" is health data (R14)
        Snapshot saved = links.findSnapshot(userId).orElseThrow(() -> new ProfileException(ProfileErrorCode.USER_NOT_FOUND));
        return toDto(userId, saved);
    }

    /** Research R5 step 2: an id the person already has is kept even when the catalogue has withdrawn it since. */
    private void validate(DietaryProfileUpdateRequest request, Snapshot current) {
        List<FieldCode> errors = new ArrayList<>();
        if (!request.allergenIds().isEmpty()
                && !links.existingAllergenIds(request.allergenIds()).containsAll(request.allergenIds())) {
            errors.add(new FieldCode("allergenIds", "UNKNOWN_ALLERGEN"));
        }
        if (request.excludedIngredientIds().size() > MAX_EXCLUDED) {
            errors.add(new FieldCode("excludedIngredientIds", "EXCLUDED_LIMIT_EXCEEDED"));
        } else {
            Set<Long> added = added(request.excludedIngredientIds(), current.excludedIngredientIds());
            if (!added.isEmpty() && !ingredients.approvedAmong(added).containsAll(added)) {
                errors.add(new FieldCode("excludedIngredientIds", "UNKNOWN_INGREDIENT"));
            }
        }
        Set<Long> addedCuisines = added(request.preferredCuisineIds(), current.preferredCuisineIds());
        if (!addedCuisines.isEmpty() && !cuisines.activeAmong(addedCuisines).containsAll(addedCuisines)) {
            errors.add(new FieldCode("preferredCuisineIds", "UNKNOWN_CUISINE"));
        }
        if (!errors.isEmpty()) {
            throw ProfileException.invalid(errors);
        }
    }

    private static Set<Long> added(Set<Long> requested, Set<Long> stored) {
        Set<Long> added = new HashSet<>(requested);
        added.removeAll(stored);
        return added;
    }

    private static boolean sameContent(Snapshot current, DietaryProfileUpdateRequest request) {
        return current.dietType() == request.dietType()
                && Objects.equals(current.householdSize(), request.householdSize())
                && Objects.equals(current.maxCookTimeMinutes(), request.maxCookTimeMinutes())
                && current.allergenIds().equals(request.allergenIds())
                && current.excludedIngredientIds().equals(request.excludedIngredientIds())
                && current.preferredCuisineIds().equals(request.preferredCuisineIds());
    }

    private DietaryProfileDto toDto(long userId, Snapshot snapshot) {
        List<AllergenDto> allergens = links.findAllergens(snapshot.allergenIds()).stream()
                .map(row -> new AllergenDto(row.id(), row.code(), row.nameVi())).toList();
        Map<Long, String> ingredientNames = ingredients.namesOf(snapshot.excludedIngredientIds());
        List<IngredientSuggestionDto> excluded = ingredientNames.entrySet().stream()
                .map(entry -> new IngredientSuggestionDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(IngredientSuggestionDto::name).thenComparing(IngredientSuggestionDto::id)).toList();
        Map<Long, String> cuisineNames = cuisines.namesOf(snapshot.preferredCuisineIds());
        List<CuisineDto> preferred = cuisineNames.entrySet().stream()
                .map(entry -> new CuisineDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CuisineDto::name).thenComparing(CuisineDto::id)).toList();
        List<DietaryNoticeDto> pending = notices.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(DietaryProfileService::toDto).toList();
        boolean empty = snapshot.dietType() == DietType.NONE && snapshot.householdSize() == null
                && snapshot.maxCookTimeMinutes() == null && snapshot.allergenIds().isEmpty()
                && snapshot.excludedIngredientIds().isEmpty() && snapshot.preferredCuisineIds().isEmpty();
        return new DietaryProfileDto(allergens, excluded, snapshot.dietType(), preferred, snapshot.householdSize(),
                snapshot.maxCookTimeMinutes(), empty, snapshot.dietaryVersion(), pending);
    }

    private static DietaryNoticeDto toDto(DietaryNotice notice) {
        return new DietaryNoticeDto(notice.getId(), notice.getKind(), notice.getSubjectName(),
                notice.getReplacementName(), notice.getCreatedAt());
    }
}
