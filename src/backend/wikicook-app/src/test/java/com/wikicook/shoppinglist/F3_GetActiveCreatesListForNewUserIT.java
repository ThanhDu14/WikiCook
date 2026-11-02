package com.wikicook.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class F3_GetActiveCreatesListForNewUserIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Long testUserId = 9301L;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM shopping_lists WHERE user_id = ?", testUserId);
        jdbcTemplate.update("INSERT INTO users (id, email, display_name) OVERRIDING SYSTEM VALUE VALUES (?, 'f3_user@example.com', 'F3 User') ON CONFLICT (id) DO NOTHING", testUserId);
    }

    @Test
    @DisplayName("F3: GET /active for user without active list must return 200 and create list")
    void getActiveCreatesListForNewUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(testUserId));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = http.exchange(
                "/api/v1/shopping-lists/active",
                HttpMethod.GET,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).as("GET /active for new user should return 200 OK").isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"ACTIVE\"");
    }
}
