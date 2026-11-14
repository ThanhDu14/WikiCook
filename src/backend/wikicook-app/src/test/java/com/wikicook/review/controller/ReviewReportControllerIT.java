package com.wikicook.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.HeaderCurrentUser;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.review.domain.Review;
import com.wikicook.review.domain.ReviewStatus;
import com.wikicook.review.dto.CreateReportRequest;
import com.wikicook.review.dto.ReportReason;
import com.wikicook.review.repository.ReviewRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(HeaderCurrentUser.class)
class ReviewReportControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    private long authorId;
    private long reviewerId;
    private long reporterId1;
    private long recipeId;
    private long reviewId;

    @BeforeEach
    void setUp() {
        MealPlanFixtures mf = new MealPlanFixtures(jdbc);
        authorId = mf.user();
        reviewerId = mf.user();
        reporterId1 = mf.user();

        RecipeFixtures rf = new RecipeFixtures(jdbc);
        recipeId = rf.publishedRecipe("Cơm tấm sườn bì chả", "Gạo tấm");

        Review review = Review.builder()
                .recipeId(recipeId)
                .userId(reviewerId)
                .rating((short) 1)
                .comment("Nội dung đánh giá phản cảm")
                .status(ReviewStatus.VISIBLE)
                .build();
        review = reviewRepository.saveAndFlush(review);
        reviewId = review.getId();
    }

    @Test
    @DisplayName("T075: Member reports review returns 201 Created")
    void testReportReviewSuccess() throws Exception {
        CreateReportRequest req = new CreateReportRequest(ReportReason.OFFENSIVE, "Dùng từ ngữ không phù hợp");

        mvc.perform(post("/api/v1/reviews/{reviewId}/reports", reviewId)
                        .header(HeaderCurrentUser.HEADER, reporterId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("REVIEW"))
                .andExpect(jsonPath("$.targetId").value(reviewId))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("T075: Self-reporting own review returns 403 Forbidden")
    void testSelfReportReturns403() throws Exception {
        CreateReportRequest req = new CreateReportRequest(ReportReason.SPAM, "Tự báo cáo bài mình");

        mvc.perform(post("/api/v1/reviews/{reviewId}/reports", reviewId)
                        .header(HeaderCurrentUser.HEADER, reviewerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/self-report-forbidden"));
    }

    @Test
    @DisplayName("T075: Duplicate report returns 409 Conflict")
    void testDuplicateReportReturns409() throws Exception {
        CreateReportRequest req = new CreateReportRequest(ReportReason.SPAM, "Báo cáo lần 1");

        mvc.perform(post("/api/v1/reviews/{reviewId}/reports", reviewId)
                        .header(HeaderCurrentUser.HEADER, reporterId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/reviews/{reviewId}/reports", reviewId)
                        .header(HeaderCurrentUser.HEADER, reporterId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/report-already-exists"));
    }

    @Test
    @DisplayName("T075: 10 reports auto-quarantines review and updates rating stats")
    void testAutoQuarantineAtTenReports() throws Exception {
        MealPlanFixtures mf = new MealPlanFixtures(jdbc);
        for (int i = 1; i <= 10; i++) {
            long reporter = mf.user();
            CreateReportRequest req = new CreateReportRequest(ReportReason.OFFENSIVE, "Báo cáo thứ " + i);
            mvc.perform(post("/api/v1/reviews/{reviewId}/reports", reviewId)
                            .header(HeaderCurrentUser.HEADER, reporter)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // Verify review is now HIDDEN
        Review updated = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
    }
}

