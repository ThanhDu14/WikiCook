package com.wikicook.aiplanner.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingTextBuilderTest {

    private final EmbeddingTextBuilder builder = new EmbeddingTextBuilder();

    private static RecipeEmbeddingSource pho(List<String> ingredients) {
        return new RecipeEmbeddingSource(7L, "Phở bò tái", "Món chính", "Việt Nam", true, true, false, 90, ingredients);
    }

    @Test
    void textHasNameCategoryCuisineMealsTimeAndIngredients() {
        EmbeddingText text = builder.build(pho(List.of("Bánh phở", "Thịt bò", "Hành lá")));

        assertThat(text.text()).isEqualTo("""
                Món: Phở bò tái
                Danh mục: Món chính
                Ẩm thực: Việt Nam
                Bữa: sáng, trưa
                Thời gian: 90 phút
                Nguyên liệu: Bánh phở, Thịt bò, Hành lá""");
    }

    @Test
    void missingPartsAreLeftOutAndIngredientsAreDeduplicated() {
        var source = new RecipeEmbeddingSource(8L, "  Chè đậu xanh ", null, null, false, false, false, null,
                List.of("Đậu xanh", " Đường ", "Đậu xanh", ""));

        assertThat(builder.build(source).text()).isEqualTo("""
                Món: Chè đậu xanh
                Nguyên liệu: Đậu xanh, Đường""");
    }

    @Test
    void hashIsSha256OfTheTextAndIgnoresTheRecipeId() {
        EmbeddingText first = builder.build(pho(List.of("Bánh phở", "Thịt bò")));
        var sameContent = new RecipeEmbeddingSource(99L, "Phở bò tái", "Món chính", "Việt Nam", true, true, false, 90,
                List.of("Bánh phở", "Thịt bò"));

        assertThat(first.contentHash()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(builder.build(sameContent).contentHash()).isEqualTo(first.contentHash());
        assertThat(builder.build(pho(List.of("Bánh phở", "Thịt gà"))).contentHash()).isNotEqualTo(first.contentHash());
    }
}
