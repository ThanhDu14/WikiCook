package com.wikicook.aiplanner.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Builds the text embedded for a recipe: name, category, cuisine, meals, total time and ingredient names
 * (research R10). Same content gives the same hash, so an unchanged recipe is never embedded twice.
 */
@Component
public class EmbeddingTextBuilder {

    public EmbeddingText build(RecipeEmbeddingSource source) {
        List<String> lines = new ArrayList<>();
        lines.add("Món: " + source.title().trim());
        addIfPresent(lines, "Danh mục: ", source.category());
        addIfPresent(lines, "Ẩm thực: ", source.cuisine());
        List<String> meals = new ArrayList<>();
        if (source.forBreakfast()) {
            meals.add("sáng");
        }
        if (source.forLunch()) {
            meals.add("trưa");
        }
        if (source.forDinner()) {
            meals.add("tối");
        }
        if (!meals.isEmpty()) {
            lines.add("Bữa: " + String.join(", ", meals));
        }
        if (source.totalTimeMinutes() != null) {
            lines.add("Thời gian: " + source.totalTimeMinutes() + " phút");
        }
        Set<String> ingredients = new LinkedHashSet<>();
        for (String name : source.ingredients()) {
            if (name != null && !name.isBlank()) {
                ingredients.add(name.trim());
            }
        }
        if (!ingredients.isEmpty()) {
            lines.add("Nguyên liệu: " + String.join(", ", ingredients));
        }
        String text = String.join("\n", lines);
        return new EmbeddingText(text, sha256(text));
    }

    private static void addIfPresent(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(label + value.trim());
        }
    }

    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
