package com.wikicook.aiplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.aiplanner.domain.RecipeEmbedding;
import com.wikicook.aiplanner.embedding.RecipeEmbeddingSource;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RecipeEmbeddingRepositoryIT extends AbstractPostgresIT {

    @Autowired
    RecipeEmbeddingRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    private RecipeFixtures recipes;

    @BeforeEach
    void seed() {
        RecipeFixtures.clear(jdbc);
        recipes = new RecipeFixtures(jdbc);
    }

    private static float[] vector(float first) {
        float[] v = new float[768];
        Arrays.fill(v, 0.01f);
        v[0] = first;
        return v;
    }

    @Test
    void readsPublishedRecipesWithLookupsIngredientsAndCurrentEmbedding() {
        long pho = recipes.publishedRecipe("Phở bò tái", "Bánh phở", "Thịt bò", "Hành lá");
        recipes.recipe("Bản nháp chưa xong", "DRAFT");
        long che = recipes.publishedRecipe("Chè đậu xanh", "Đậu xanh");
        repository.upsert(che, vector(0.5f), "gemini-embedding-001", "b".repeat(64));

        List<RecipeEmbeddingRepository.PublishedRecipe> page = repository.findPublishedAfter(0, 10);

        assertThat(page).extracting(p -> p.source().recipeId()).containsExactly(pho, che);
        RecipeEmbeddingSource source = page.getFirst().source();
        assertThat(source.title()).isEqualTo("Phở bò tái");
        assertThat(source.category()).isEqualTo("Món chính");
        assertThat(source.cuisine()).isEqualTo("Việt Nam");
        assertThat(source.forBreakfast()).isFalse();
        assertThat(source.forLunch()).isTrue();
        assertThat(source.totalTimeMinutes()).isEqualTo(40);
        assertThat(source.ingredients()).containsExactly("Bánh phở", "Thịt bò", "Hành lá");
        assertThat(page.getFirst().contentHash()).isNull();
        assertThat(page.get(1).contentHash()).isEqualTo("b".repeat(64));
        assertThat(page.get(1).model()).isEqualTo("gemini-embedding-001");
    }

    @Test
    void pagesById() {
        long first = recipes.publishedRecipe("Món thứ nhất");
        long second = recipes.publishedRecipe("Món thứ hai");
        long third = recipes.publishedRecipe("Món thứ ba");

        assertThat(repository.findPublishedAfter(0, 2)).extracting(p -> p.source().recipeId())
                .containsExactly(first, second);
        assertThat(repository.findPublishedAfter(second, 2)).extracting(p -> p.source().recipeId())
                .containsExactly(third);
    }

    @Test
    void upsertInsertsThenReplaces() {
        long pho = recipes.publishedRecipe("Phở bò tái", "Bánh phở");

        repository.upsert(pho, vector(0.1f), "model-a", "a".repeat(64));
        repository.upsert(pho, vector(0.9f), "model-b", "c".repeat(64));

        RecipeEmbedding stored = repository.findByRecipeId(pho).orElseThrow();
        assertThat(stored.model()).isEqualTo("model-b");
        assertThat(stored.contentHash()).isEqualTo("c".repeat(64));
        assertThat(stored.embedding()).hasSize(768);
        assertThat(stored.embedding()[0]).isEqualTo(0.9f);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM recipe_embeddings", Integer.class)).isEqualTo(1);
    }

    @Test
    void deletesRowsOfRecipesNoLongerPublished() {
        long kept = recipes.publishedRecipe("Phở bò tái");
        long archived = recipes.publishedRecipe("Bún chả Hà Nội");
        repository.upsert(kept, vector(0.1f), "m", "a".repeat(64));
        repository.upsert(archived, vector(0.1f), "m", "a".repeat(64));
        recipes.archive(archived);

        assertThat(repository.deleteNotPublished()).isEqualTo(1);
        assertThat(repository.findByRecipeId(kept)).isPresent();
        assertThat(repository.findByRecipeId(archived)).isEmpty();

        assertThat(repository.deleteByRecipeId(kept)).isEqualTo(1);
        assertThat(repository.findByRecipeId(kept)).isEmpty();
    }

    @Test
    void findsPublishedRecipesWithoutEmbedding() {
        long embedded = recipes.publishedRecipe("Phở bò tái");
        long missing = recipes.publishedRecipe("Cơm tấm sườn");
        recipes.recipe("Bản nháp chưa xong", "DRAFT");
        repository.upsert(embedded, vector(0.1f), "m", "a".repeat(64));

        assertThat(repository.findPublishedWithoutEmbedding(10)).containsExactly(missing);
    }
}
