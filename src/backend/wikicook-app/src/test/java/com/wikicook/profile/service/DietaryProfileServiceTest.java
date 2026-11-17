package com.wikicook.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.profile.dto.DietaryProfileDto;
import com.wikicook.profile.dto.DietaryProfileUpdateRequest;
import com.wikicook.profile.port.CuisineCatalogPort;
import com.wikicook.profile.port.IngredientCatalogPort;
import com.wikicook.profile.port.ProfileAuditPort;
import com.wikicook.profile.repository.DietaryLinkRepository;
import com.wikicook.profile.repository.DietaryLinkRepository.Snapshot;
import com.wikicook.profile.repository.DietaryNoticeRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** T027: the rules of research R5 around the repository, without a database. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DietaryProfileServiceTest {

    private static final long USER = 7;

    @Mock
    DietaryLinkRepository links;
    @Mock
    DietaryProfileReader reader;
    @Mock
    IngredientCatalogPort ingredients;
    @Mock
    CuisineCatalogPort cuisines;
    @Mock
    DietaryNoticeRepository notices;
    @Mock
    ProfileAuditPort audit;

    DietaryProfileService service;

    @BeforeEach
    void setUp() {
        service = new DietaryProfileService(links, reader, ingredients, cuisines, notices, audit);
        when(links.findSnapshot(USER)).thenReturn(java.util.Optional.of(snapshot(0, Set.of(), Set.of(), Set.of())));
        when(links.existingAllergenIds(any())).thenAnswer(call -> echo(call.getArgument(0)));
        when(ingredients.approvedAmong(any())).thenAnswer(call -> echo(call.getArgument(0)));
        when(cuisines.activeAmong(any())).thenAnswer(call -> echo(call.getArgument(0)));
        when(links.updateScalars(anyLong(), anyInt(), any(), any(), any())).thenReturn(1);
    }

    /** The ids asked about; re-stubbing with any() calls the answer with null, so null is an empty set. */
    private static Set<Long> echo(java.util.Collection<Long> ids) {
        return ids == null ? Set.of() : Set.copyOf(ids);
    }

    private static Snapshot snapshot(int version, Set<Long> allergens, Set<Long> excluded, Set<Long> cuisines) {
        return new Snapshot(DietType.NONE, null, null, version, allergens, excluded, cuisines);
    }

    private static DietaryProfileUpdateRequest request(Set<Long> allergens, Set<Long> excluded, Set<Long> cuisines,
            Integer household, int version) {
        return new DietaryProfileUpdateRequest(allergens, excluded, DietType.NONE, cuisines, household, null, version);
    }

    private ProfileException refusal(DietaryProfileUpdateRequest request) {
        return org.junit.jupiter.api.Assertions.assertThrows(ProfileException.class, () -> service.save(USER, request));
    }

    @Test
    @DisplayName("Edge: Invalid data - an allergen id that is not in the fixed list is UNKNOWN_ALLERGEN")
    void unknownAllergenIsRefused() {
        when(links.existingAllergenIds(Set.of(1L, 999L))).thenReturn(Set.of(1L));

        ProfileException refused = refusal(request(Set.of(1L, 999L), Set.of(), Set.of(), null, 0));

        assertThat(refused.code()).isEqualTo(ProfileErrorCode.VALIDATION_ERROR);
        assertThat(refused.errors()).containsExactly(new ProfileException.FieldCode("allergenIds", "UNKNOWN_ALLERGEN"));
        verify(links, never()).updateScalars(anyLong(), anyInt(), any(), any(), any());
        verify(audit, never()).record(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Edge: Invalid data - a NEW excluded ingredient must be APPROVED; one already saved is kept")
    void onlyNewIngredientsMustBeApproved() {
        when(links.findSnapshot(USER)).thenReturn(java.util.Optional.of(snapshot(3, Set.of(), Set.of(5L), Set.of())));
        when(ingredients.approvedAmong(Set.of(6L, 7L))).thenReturn(Set.of(6L));

        ProfileException refused = refusal(request(Set.of(), Set.of(5L, 6L, 7L), Set.of(), null, 3));

        assertThat(refused.errors()).containsExactly(new ProfileException.FieldCode("excludedIngredientIds", "UNKNOWN_INGREDIENT"));
        verify(ingredients).approvedAmong(Set.of(6L, 7L)); // 5 is not asked about again
    }

    @Test
    @DisplayName("Edge: Invalid data - an ingredient saved earlier stays even after it is no longer approved")
    void savedIngredientStaysWhenNoLongerApproved() {
        when(links.findSnapshot(USER)).thenReturn(java.util.Optional.of(snapshot(3, Set.of(), Set.of(5L), Set.of())));
        when(ingredients.approvedAmong(any())).thenReturn(Set.of()); // 5 was withdrawn from the catalogue

        service.save(USER, request(Set.of(), Set.of(5L), Set.of(), 4, 3));

        verify(links).replaceExcludedIngredients(USER, Set.of(5L));
    }

    @Test
    @DisplayName("Edge: Invalid data - a NEW preferred cuisine must be active")
    void newCuisineMustBeActive() {
        when(cuisines.activeAmong(Set.of(10L))).thenReturn(Set.of());

        ProfileException refused = refusal(request(Set.of(), Set.of(), Set.of(10L), null, 0));

        assertThat(refused.errors()).containsExactly(new ProfileException.FieldCode("preferredCuisineIds", "UNKNOWN_CUISINE"));
    }

    @Test
    @DisplayName("FR-016: every wrong field is reported at once, nothing is written")
    void allErrorsAreReportedTogether() {
        when(links.existingAllergenIds(any())).thenReturn(Set.of());
        when(cuisines.activeAmong(any())).thenReturn(Set.of());

        ProfileException refused = refusal(request(Set.of(1L), Set.of(), Set.of(10L), null, 0));

        assertThat(refused.errors()).containsExactlyInAnyOrder(
                new ProfileException.FieldCode("allergenIds", "UNKNOWN_ALLERGEN"),
                new ProfileException.FieldCode("preferredCuisineIds", "UNKNOWN_CUISINE"));
        verify(links, never()).replaceAllergens(anyLong(), any());
    }

    @Test
    @DisplayName("FR-011: more than 50 excluded ingredients is EXCLUDED_LIMIT_EXCEEDED even if the DTO check was skipped")
    void fiftyOneExcludedIsRefused() {
        Set<Long> fiftyOne = java.util.stream.LongStream.rangeClosed(1, 51).boxed().collect(java.util.stream.Collectors.toSet());

        ProfileException refused = refusal(request(Set.of(), fiftyOne, Set.of(), null, 0));

        assertThat(refused.errors()).containsExactly(new ProfileException.FieldCode("excludedIngredientIds", "EXCLUDED_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("FR-041: identical content writes nothing and records nothing, even with an old version")
    void identicalContentIsANoOp() {
        when(links.findSnapshot(USER)).thenReturn(java.util.Optional.of(
                snapshot(5, Set.of(1L), Set.of(2L), Set.of(3L))));

        DietaryProfileDto result = service.save(USER, request(Set.of(1L), Set.of(2L), Set.of(3L), null, 0 /* stale */));

        assertThat(result.version()).isEqualTo(5);
        verify(links, never()).updateScalars(anyLong(), anyInt(), any(), any(), any());
        verify(links, never()).replaceAllergens(anyLong(), any());
        verify(audit, never()).record(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Edge: Two tabs - different content with an old version is PROFILE_CHANGED and writes no link row")
    void staleVersionWithNewContentIsAConflict() {
        when(links.findSnapshot(USER)).thenReturn(java.util.Optional.of(snapshot(5, Set.of(), Set.of(), Set.of())));
        when(links.updateScalars(eq(USER), eq(4), any(), any(), any())).thenReturn(0);

        ProfileException refused = refusal(request(Set.of(1L), Set.of(), Set.of(), null, 4));

        assertThat(refused.code()).isEqualTo(ProfileErrorCode.PROFILE_CHANGED);
        verify(links, never()).replaceAllergens(anyLong(), any());
        verify(audit, never()).record(anyLong(), any(), any());
    }

    @Test
    @DisplayName("R5: a changed profile updates the row, replaces the three lists and audits once without details")
    void changedProfileIsSavedAndAudited() {
        when(links.findSnapshot(USER)).thenReturn(
                java.util.Optional.of(snapshot(0, Set.of(), Set.of(), Set.of())),
                java.util.Optional.of(new Snapshot(DietType.VEGAN, 4, 45, 1, Set.of(1L), Set.of(2L), Set.of(3L))));
        when(ingredients.namesOf(Set.of(2L))).thenReturn(Map.of(2L, "Ngò rí"));
        when(cuisines.namesOf(Set.of(3L))).thenReturn(Map.of(3L, "Việt Nam"));
        when(links.findAllergens(Set.of(1L))).thenReturn(List.of(new DietaryLinkRepository.AllergenRow(1L, "PEANUT", "Đậu phộng")));

        DietaryProfileDto saved = service.save(USER, new DietaryProfileUpdateRequest(
                Set.of(1L), Set.of(2L), DietType.VEGAN, Set.of(3L), 4, 45, 0));

        verify(links).updateScalars(USER, 0, DietType.VEGAN, 4, 45);
        verify(links).replaceAllergens(USER, Set.of(1L));
        verify(links).replaceExcludedIngredients(USER, Set.of(2L));
        verify(links).replaceCuisines(USER, Set.of(3L));
        verify(audit).record(USER, "DIETARY_PROFILE_UPDATED", Map.of());
        assertThat(saved.version()).isEqualTo(1);
        assertThat(saved.empty()).isFalse();
        assertThat(saved.excludedIngredients()).extracting("name").containsExactly("Ngò rí");
        assertThat(saved.preferredCuisines()).extracting("name").containsExactly("Việt Nam");
        assertThat(saved.allergens()).extracting("code").containsExactly("PEANUT");
    }

    @Test
    @DisplayName("US1-7: clearing every field is valid and the result is empty")
    void clearingEverythingIsValid() {
        when(links.findSnapshot(USER)).thenReturn(
                java.util.Optional.of(new Snapshot(DietType.VEGAN, 4, 45, 2, Set.of(1L), Set.of(), Set.of())),
                java.util.Optional.of(snapshot(3, Set.of(), Set.of(), Set.of())));

        DietaryProfileDto saved = service.save(USER, request(Set.of(), Set.of(), Set.of(), null, 2));

        verify(links).updateScalars(USER, 2, DietType.NONE, null, null);
        assertThat(saved.empty()).isTrue();
    }

    @Test
    @DisplayName("FR-008: a user without a row is USER_NOT_FOUND, not an empty profile")
    void missingUserIsNotFound() {
        when(links.findSnapshot(USER)).thenReturn(java.util.Optional.empty());

        assertThat(refusal(request(Set.of(), Set.of(), Set.of(), null, 0)).code()).isEqualTo(ProfileErrorCode.USER_NOT_FOUND);
        when(reader.read(USER)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.get(USER)).isInstanceOfSatisfying(ProfileException.class,
                e -> assertThat(e.code()).isEqualTo(ProfileErrorCode.USER_NOT_FOUND));
    }
}
