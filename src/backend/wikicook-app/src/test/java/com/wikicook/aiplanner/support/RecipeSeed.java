package com.wikicook.aiplanner.support;

import java.util.ArrayList;
import java.util.List;

/**
 * The shared catalogue for AI planner tests (T024): 60 published recipes over three cuisines and every meal, some
 * with peanuts, plus one snack and one archived recipe. Deterministic: the same data on every run.
 */
public final class RecipeSeed {

    /**
     * @param breakfast   breakfast-only recipes (20)
     * @param lunchDinner lunch and dinner recipes (30)
     * @param allDay      recipes for every meal (10)
     * @param withPeanut  published recipes containing an ingredient tagged {@code peanutAllergen}
     */
    public record Seeded(long vietnamese, long japanese, long italian, long peanutAllergen, long peanut,
                         List<Long> breakfast, List<Long> lunchDinner, List<Long> allDay, List<Long> withPeanut,
                         long snack, long archived) {

        public List<Long> published() {
            List<Long> all = new ArrayList<>(breakfast);
            all.addAll(lunchDinner);
            all.addAll(allDay);
            return all;
        }
    }

    private RecipeSeed() {
    }

    public static Seeded seed(RecipeFixtures f) {
        long[] cuisines = {f.cuisine("Việt Nam"), f.cuisine("Nhật Bản"), f.cuisine("Ý")};
        long[] categories = {f.category("Món chính"), f.category("Món nước"), f.category("Món chay")};
        long peanutAllergen = f.allergen("PEANUT");
        long peanut = f.ingredient("Đậu phộng", "PLANT");
        f.tagAllergen(peanut, peanutAllergen);
        long[] pool = {
                f.ingredient("Gạo", "PLANT"), f.ingredient("Thịt heo", "MEAT"), f.ingredient("Thịt bò", "MEAT"),
                f.ingredient("Tôm", "SEAFOOD"), f.ingredient("Nước mắm", "SEAFOOD"), f.ingredient("Trứng gà", "EGG"),
                f.ingredient("Đậu hũ", "PLANT"), f.ingredient("Rau muống", "PLANT"), f.ingredient("Hành lá", "PLANT"),
                f.ingredient("Cà chua", "PLANT"), f.ingredient("Mì", "PLANT"), f.ingredient("Sữa tươi", "DAIRY"),
        };
        List<Long> breakfast = new ArrayList<>();
        List<Long> lunchDinner = new ArrayList<>();
        List<Long> allDay = new ArrayList<>();
        List<Long> withPeanut = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            String kind = i < 20 ? "sáng" : i < 50 ? "trưa tối" : "cả ngày";
            RecipeFixtures.Builder recipe = f.newRecipe("Món %s %02d".formatted(kind, i))
                    .cuisine(cuisines[i % 3])
                    .category(categories[i % 3])
                    .minutes(5 + (i % 4) * 5, 10 + (i % 6) * 10)
                    .rating(i % 5)
                    .ingredients(pool[i % pool.length], pool[(i + 3) % pool.length], pool[(i + 5) % pool.length]);
            if (i < 20) {
                recipe.meals(true, false, false);
            } else if (i >= 50) {
                recipe.meals(true, true, true);
            }
            boolean hasPeanut = i % 7 == 0;
            if (hasPeanut) {
                recipe.ingredients(peanut);
            }
            long id = recipe.create();
            (i < 20 ? breakfast : i < 50 ? lunchDinner : allDay).add(id);
            if (hasPeanut) {
                withPeanut.add(id);
            }
        }
        long snack = f.newRecipe("Bánh tráng trộn").snack().ingredients(pool[0]).create();
        long archived = f.newRecipe("Món đã lưu trữ").status("ARCHIVED").ingredients(pool[0]).create();
        return new Seeded(cuisines[0], cuisines[1], cuisines[2], peanutAllergen, peanut, List.copyOf(breakfast),
                List.copyOf(lunchDinner), List.copyOf(allDay), List.copyOf(withPeanut), snack, archived);
    }
}
