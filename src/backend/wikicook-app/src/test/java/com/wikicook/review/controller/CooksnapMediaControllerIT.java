package com.wikicook.review.controller;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.review.domain.Cooksnap;
import com.wikicook.review.domain.Review;
import com.wikicook.review.domain.ReviewStatus;
import com.wikicook.review.repository.CooksnapRepository;
import com.wikicook.review.repository.ReviewRepository;
import com.wikicook.review.service.StorageService;
import com.wikicook.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CooksnapMediaControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CooksnapRepository cooksnapRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private JdbcTemplate jdbc;

    private long userId;
    private long recipeId;

    @BeforeEach
    void setUp() {
        MealPlanFixtures mf = new MealPlanFixtures(jdbc);
        userId = mf.user();

        RecipeFixtures rf = new RecipeFixtures(jdbc);
        recipeId = rf.publishedRecipe("Gỏi cuốn tôm thịt", "Bánh tráng");
    }

    @Test
    @DisplayName("T026: Serve WebP image with Cache-Control and ETag headers")
    void testServeCooksnapWebp() throws Exception {
        String key = "cs_it_" + UUID.randomUUID() + ".webp";
        byte[] fullBytes = new byte[]{1, 2, 3, 4};
        byte[] thumbBytes = new byte[]{5, 6};
        storageService.store(key, fullBytes, thumbBytes);

        Review review = Review.builder()
                .recipeId(recipeId)
                .userId(userId)
                .rating((short) 5)
                .comment("Gỏi cuốn rất ngon và thanh mát!")
                .status(ReviewStatus.VISIBLE)
                .build();
        reviewRepository.saveAndFlush(review);

        Cooksnap cooksnap = Cooksnap.builder()
                .recipeId(recipeId)
                .review(review)
                .userId(userId)
                .storageKey(key)
                .contentType("image/webp")
                .sizeBytes(fullBytes.length)
                .status(ReviewStatus.VISIBLE)
                .build();
        cooksnapRepository.saveAndFlush(cooksnap);

        // Fetch full image
        mvc.perform(get("/api/v1/media/cooksnaps/{storageKey}", key))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(header().exists("ETag"));

        // Fetch thumbnail variant
        mvc.perform(get("/api/v1/media/cooksnaps/{storageKey}", key).param("thumbnail", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(thumbBytes));
    }

    @Test
    @DisplayName("T026: Return 404 Not Found when cooksnap is HIDDEN")
    void testHiddenCooksnapReturns404() throws Exception {
        String key = "cs_hidden_" + UUID.randomUUID() + ".webp";
        storageService.store(key, new byte[]{1, 2}, null);

        Review review = Review.builder()
                .recipeId(recipeId)
                .userId(userId)
                .rating((short) 5)
                .comment("Bị ẩn kiểm duyệt")
                .status(ReviewStatus.VISIBLE)
                .build();
        reviewRepository.saveAndFlush(review);

        Cooksnap cooksnap = Cooksnap.builder()
                .recipeId(recipeId)
                .review(review)
                .userId(userId)
                .storageKey(key)
                .contentType("image/webp")
                .sizeBytes(2)
                .status(ReviewStatus.HIDDEN) // Hidden
                .build();
        cooksnapRepository.saveAndFlush(cooksnap);

        mvc.perform(get("/api/v1/media/cooksnaps/{storageKey}", key))
                .andExpect(status().isNotFound());
    }
}

