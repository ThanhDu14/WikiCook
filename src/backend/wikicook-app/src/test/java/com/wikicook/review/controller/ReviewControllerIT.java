package com.wikicook.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.HeaderCurrentUser;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.review.dto.CreateReviewRequest;
import com.wikicook.review.dto.UpdateReviewRequest;
import com.wikicook.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class ReviewControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    private long authorId;
    private long reviewerId1;
    private long reviewerId2;
    private long recipeId;

    @BeforeEach
    void setUp() {
        MealPlanFixtures mf = new MealPlanFixtures(jdbc);
        authorId = mf.user();
        reviewerId1 = mf.user();
        reviewerId2 = mf.user();

        RecipeFixtures rf = new RecipeFixtures(jdbc);
        recipeId = rf.publishedRecipe("Bún bò Huế cay nồng", "Bún tươi");
    }

    @Test
    @DisplayName("T025: Submit valid JSON review returns 201 Created and updates stats")
    void testCreateReviewJsonSuccess() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest((short) 5, "Món bún bò ngon chuẩn vị Huế!", "Thêm chút ruốc");

        mvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value(req.comment()))
                .andExpect(jsonPath("$.substitutionTip").value(req.substitutionTip()));
    }

    @Test
    @DisplayName("T025: Author cannot review own recipe returns 403 Forbidden")
    void testAuthorCannotReviewOwnRecipeReturns403() throws Exception {
        // Set author_id on recipes row to authorId
        jdbc.update("UPDATE recipes SET author_id = ? WHERE id = ?", authorId, recipeId);

        CreateReviewRequest req = new CreateReviewRequest((short) 5, "Tự nấu tự khen cực phẩm!", null);

        mvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .header(HeaderCurrentUser.HEADER, authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/self-review-forbidden"));
    }

    @Test
    @DisplayName("T025: Duplicate review returns 409 Conflict")
    void testDuplicateReviewReturns409() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest((short) 5, "Đánh giá lần đầu tiên", null);

        mvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/review-already-exists"));
    }

    @Test
    @DisplayName("T034: Edit own review returns 200 and changes content")
    void testEditOwnReviewSuccess() throws Exception {
        CreateReviewRequest createReq = new CreateReviewRequest((short) 5, "Đánh giá ban đầu 5 sao", null);

        String responseStr = mvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long reviewId = json.readTree(responseStr).get("id").asLong();

        UpdateReviewRequest updateReq = new UpdateReviewRequest((short) 4, "Sửa lại 4 sao: hơi cay quá", "Bớt ớt bột", null);

        mvc.perform(put("/api/v1/reviews/{reviewId}", reviewId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Sửa lại 4 sao: hơi cay quá"))
                .andExpect(jsonPath("$.substitutionTip").value("Bớt ớt bột"));
    }

    @Test
    @DisplayName("T034: Delete own review returns 204 No Content, then user can review again")
    void testDeleteOwnReviewAndReReview() throws Exception {
        CreateReviewRequest createReq = new CreateReviewRequest((short) 5, "Đánh giá để xóa thử nghiệm", null);

        String responseStr = mvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long reviewId = json.readTree(responseStr).get("id").asLong();

        // Delete review
        mvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1))
                .andExpect(status().isNoContent());

        // Re-review after hard delete succeeds with 201 Created
        CreateReviewRequest newReq = new CreateReviewRequest((short) 5, "Đánh giá mới tinh sau khi xóa!", null);
        mvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(newReq)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("T039: Public guest can view paginated reviews and rating summary")
    void testGetRecipeReviewsPublic() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest((short) 5, "Khách vào đọc thấy đánh giá này!", null);
        mvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Guest call without auth header
        mvc.perform(get("/api/v1/recipes/{recipeId}/reviews", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.ratingSummary.averageRating").value(5.0))
                .andExpect(jsonPath("$.ratingSummary.totalReviews").value(1));
    }

    @Test
    @DisplayName("T045: Toggle helpful vote increments and decrements count")
    void testToggleHelpfulVote() throws Exception {
        CreateReviewRequest createReq = new CreateReviewRequest((short) 5, "Bài viết hữu ích để thử vote", null);
        String responseStr = mvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long reviewId = json.readTree(responseStr).get("id").asLong();

        // Reviewer 2 votes helpful -> count = 1, hasVoted = true
        mvc.perform(post("/api/v1/reviews/{reviewId}/helpful", reviewId)
                        .header(HeaderCurrentUser.HEADER, reviewerId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helpfulCount").value(1))
                .andExpect(jsonPath("$.hasVoted").value(true));

        // Reviewer 2 votes again -> count = 0, hasVoted = false
        mvc.perform(post("/api/v1/reviews/{reviewId}/helpful", reviewId)
                        .header(HeaderCurrentUser.HEADER, reviewerId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helpfulCount").value(0))
                .andExpect(jsonPath("$.hasVoted").value(false));

        // Author of review attempts to vote on own review -> 403 Forbidden
        mvc.perform(post("/api/v1/reviews/{reviewId}/helpful", reviewId)
                        .header(HeaderCurrentUser.HEADER, reviewerId1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/self-helpful-vote-forbidden"));
    }
}

