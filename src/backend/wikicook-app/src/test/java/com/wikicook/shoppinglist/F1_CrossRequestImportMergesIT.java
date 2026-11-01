package com.wikicook.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class F1_CrossRequestImportMergesIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final Long testUserId = 9101L;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM shopping_lists WHERE user_id = ?", testUserId);
        jdbcTemplate.update("INSERT INTO users (id, email, display_name) OVERRIDING SYSTEM VALUE VALUES (?, 'f1_user@example.com', 'F1 User') ON CONFLICT (id) DO NOTHING", testUserId);

        // Ensure baseline lookups and catalogue fixtures exist for the test
        jdbcTemplate.update("""
            INSERT INTO aisle_categories (id, code, name_vi, position) OVERRIDING SYSTEM VALUE
            VALUES
                (1, 'PRODUCE', 'Rau củ & Trái cây', 1),
                (2, 'MEAT_SEAFOOD', 'Thịt & Thủy hải sản', 2),
                (3, 'DAIRY_EGGS', 'Trứng & Sữa', 3),
                (4, 'PANTRY_SPICES', 'Gia vị & Đồ khô', 4),
                (5, 'FROZEN', 'Đồ đông lạnh', 5),
                (6, 'GENERAL', 'Đồ dùng khác', 6)
            ON CONFLICT (id) DO NOTHING
        """);

        jdbcTemplate.update("""
            INSERT INTO units (id, code, name_vi, unit_type) OVERRIDING SYSTEM VALUE
            VALUES
                (1, 'g', 'g', 'MASS'),
                (2, 'kg', 'kg', 'MASS'),
                (3, 'ml', 'ml', 'VOLUME'),
                (4, 'l', 'l', 'VOLUME'),
                (5, 'qua', 'quả', 'COUNT'),
                (6, 'cu', 'củ', 'COUNT'),
                (7, 'cay', 'cây', 'COUNT')
            ON CONFLICT (id) DO NOTHING
        """);

        jdbcTemplate.update("""
            INSERT INTO ingredients (id, name, name_normalized, kind, status) OVERRIDING SYSTEM VALUE
            VALUES
                (1, 'Thịt bò', 'thit bo', 'MEAT', 'APPROVED'),
                (2, 'Cà rốt', 'ca rot', 'PLANT', 'APPROVED'),
                (3, 'Sả', 'sa', 'PLANT', 'APPROVED'),
                (4, 'Cần tây', 'can tay', 'PLANT', 'APPROVED'),
                (5, 'Tỏi', 'toi', 'PLANT', 'APPROVED')
            ON CONFLICT (id) DO NOTHING
        """);

        jdbcTemplate.update("""
            INSERT INTO recipes (id, title, status) OVERRIDING SYSTEM VALUE
            VALUES
                (1, 'Bò kho', 'DRAFT'),
                (2, 'Bò xào cần tây', 'DRAFT')
            ON CONFLICT (id) DO NOTHING
        """);
    }

    @Test
    @DisplayName("F1: cross-request recipe imports must merge identical ingredients (600g + 400g = 1 kg) without null displayUnit")
    void crossRequestImportMergesIngredientWithDisplayUnit() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(testUserId));
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Request 1: Import recipe 1 (Bò kho, base 2 servings, scaled x4 -> 600g Thịt bò)
        String body1 = "{\"recipeId\": 1, \"targetServings\": 4}";
        ResponseEntity<String> resp1 = http.exchange(
                "/api/v1/shopping-lists/import/recipe",
                HttpMethod.POST,
                new HttpEntity<>(body1, headers),
                String.class
        );
        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Request 2: Import recipe 2 (Bò xào cần tây, base 4 servings, scaled x4 -> 400g Thịt bò)
        String body2 = "{\"recipeId\": 2, \"targetServings\": 4}";
        ResponseEntity<String> resp2 = http.exchange(
                "/api/v1/shopping-lists/import/recipe",
                HttpMethod.POST,
                new HttpEntity<>(body2, headers),
                String.class
        );
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Retrieve active list and inspect items
        ResponseEntity<String> activeResp = http.exchange(
                "/api/v1/shopping-lists/active",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(activeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode root = objectMapper.readTree(activeResp.getBody());
        List<JsonNode> beefItems = new ArrayList<>();
        for (JsonNode group : root.path("aisleGroups")) {
            for (JsonNode item : group.path("items")) {
                if (item.path("ingredientId").asLong() == 1L) {
                    beefItems.add(item);
                }
            }
        }

        // Assert consolidation: exactly 1 merged item of 1 kg Thịt bò
        assertThat(beefItems).as("Thịt bò should be merged into exactly 1 item across requests").hasSize(1);
        JsonNode beef = beefItems.get(0);
        assertThat(beef.path("displayQuantity").asDouble()).as("600g + 400g should aggregate to 1 kg").isEqualTo(1.0);
        assertThat(beef.path("displayUnit").asText()).as("displayUnit must not be null or empty").isEqualTo("kg");
    }
}
