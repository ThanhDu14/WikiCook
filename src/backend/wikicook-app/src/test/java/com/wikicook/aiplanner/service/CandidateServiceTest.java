package com.wikicook.aiplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.embedding.EmbeddingClient;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.repository.CandidateQuery;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

/** T111: retrieval takes twice the places it needs and keeps a varied list (research R5). */
class CandidateServiceTest {

    private static final GenerationConstraints NONE = new GenerationConstraints(Set.of(), Set.of(), DietType.NONE,
            2, null, Set.of());
    private static final List<Long> IDS = LongStream.rangeClosed(1, 60).boxed().toList();

    private final CandidateQuery query = mock(CandidateQuery.class);
    private final EmbeddingClient embedding = mock(EmbeddingClient.class);
    private final CandidateService service = new CandidateService(query, embedding);

    /** 40 recipes, best first: the first 20 all of category 1, then one category each. */
    private static List<Candidate> crowded() {
        List<Candidate> list = new ArrayList<>();
        for (long id = 1; id <= 40; id++) {
            long category = id <= 20 ? 1 : id;
            list.add(new Candidate(id, "Món " + id, Set.of(Meal.LUNCH), 20, 1L, "Việt Nam", category,
                    List.of(1000 + id), List.of(), 0));
        }
        return list;
    }

    private static long categories(List<Candidate> candidates) {
        return candidates.stream().map(Candidate::categoryId).distinct().count();
    }

    @Test
    void vectorsAskTwiceKAndKeepKVariedRecipes() {
        when(embedding.embed(anyString(), any())).thenReturn(new float[] {1f});
        when(embedding.model()).thenReturn("m");
        when(query.nearest(anyCollection(), any(), eq("m"), anyInt())).thenReturn(crowded());
        when(query.withoutEmbedding(anyCollection(), anySet(), eq("m"), anyInt())).thenReturn(List.of());

        List<Candidate> lunch = service.candidates(Map.of(Meal.LUNCH, IDS), Map.of(Meal.LUNCH, 1), NONE)
                .get(Meal.LUNCH);

        verify(query).nearest(IDS, new float[] {1f}, "m", 2 * CandidateService.MIN_K);
        assertThat(lunch).hasSize(CandidateService.MIN_K);
        assertThat(lunch.getFirst().recipeId()).isEqualTo(1L); // the closest stays first
        assertThat(categories(lunch)).isGreaterThan(categories(crowded().subList(0, CandidateService.MIN_K)));
        assertThat(lunch).extracting(Candidate::rank).containsExactlyElementsOf(
                LongStream.range(0, CandidateService.MIN_K).boxed().map(Long::intValue).toList());
    }

    @Test
    void withoutVectorsTheSqlListIsVariedToo() {
        when(embedding.embed(anyString(), any())).thenThrow(new AiUnavailableException("down"));
        when(query.bySql(anyCollection(), anySet(), anyInt())).thenReturn(crowded());

        List<Candidate> lunch = service.candidates(Map.of(Meal.LUNCH, IDS), Map.of(Meal.LUNCH, 1), NONE)
                .get(Meal.LUNCH);

        verify(query).bySql(IDS, Set.of(), 2 * CandidateService.MIN_K);
        assertThat(lunch).hasSize(CandidateService.MIN_K);
        assertThat(categories(lunch)).isGreaterThan(1);
    }
}
