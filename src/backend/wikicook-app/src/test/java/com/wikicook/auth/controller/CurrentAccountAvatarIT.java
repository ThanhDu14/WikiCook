package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The picture in {@code CurrentAccount}: {@code GET /auth/me}, sign-in and refresh carry {@code avatarUrl} when the
 * user has a picture and leave the field out when not, with the same URL as {@code GET /me/profile} (011). The picture
 * is changed through the real 011 endpoint with a real JWT, so the test also shows that {@code /auth/me} follows an
 * upload and a removal at once.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class CurrentAccountAvatarIT extends AbstractPostgresIT {

    private static final String AVATAR_URL = "/api/v1/media/avatars/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg";
    private static final Pattern COOKIE_VALUE = Pattern.compile("wc_rt=([^;]*)");

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
    }

    private JsonNode me(SignedIn as) throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/v1/auth/me").header("Authorization", as.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode myProfile(SignedIn as) throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/v1/me/profile").header("Authorization", as.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    private void saveProfile(SignedIn as, boolean removeAvatar, byte[] avatar) throws Exception {
        int version = myProfile(as).get("version").asInt();
        String profile = "{\"displayName\":\"Test Member\",\"bio\":null,\"removeAvatar\":%s,\"version\":%d}"
                .formatted(removeAvatar, version);
        var request = multipart("/api/v1/me/profile").file(
                new MockMultipartFile("profile", "", "application/json", profile.getBytes(StandardCharsets.UTF_8)));
        if (avatar != null) {
            request.file(new MockMultipartFile("avatar", "me.jpg", "image/jpeg", avatar));
        }
        mvc.perform(request.header("Authorization", as.bearer())).andExpect(status().isOk());
    }

    private static byte[] sampleJpeg() throws Exception {
        return Files.readAllBytes(Path.of("src/test/resources/avatars/plain.jpg"));
    }

    @Test
    @DisplayName("no picture: /auth/me has no avatarUrl at all, so the client shows the initials")
    void noPicture() throws Exception {
        JsonNode account = me(support.signedInMember());

        assertThat(account.has("avatarUrl")).isFalse();
        assertThat(account.has("email")).as("the rest of the contract is unchanged").isTrue();
    }

    @Test
    @DisplayName("a picture: /auth/me shows the media URL, the same one /me/profile shows, and the file is served")
    void withPicture() throws Exception {
        SignedIn member = support.signedInMember();
        saveProfile(member, false, sampleJpeg());

        String url = me(member).get("avatarUrl").asText();

        assertThat(url).matches(AVATAR_URL);
        assertThat(url).isEqualTo(myProfile(member).get("avatarUrl").asText());
        mvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a new upload changes the URL, removing the picture takes the field away again")
    void followsUploadAndRemoval() throws Exception {
        SignedIn member = support.signedInMember();
        saveProfile(member, false, sampleJpeg());
        String first = me(member).get("avatarUrl").asText();

        saveProfile(member, false, sampleJpeg());
        String second = me(member).get("avatarUrl").asText();
        assertThat(second).matches(AVATAR_URL).isNotEqualTo(first);

        saveProfile(member, true, null);
        assertThat(me(member).has("avatarUrl")).isFalse();
    }

    @Test
    @DisplayName("sign-in and refresh carry the avatarUrl of the account, null picture means no field")
    void signInAndRefresh() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long user = support.memberWithPassword(email, "Correct-horse-1");

        MockHttpServletResponse plain = login(email);
        assertThat(json.readTree(plain.getContentAsString()).get("account").has("avatarUrl")).isFalse();

        jdbc.update("UPDATE users SET avatar_storage_key = 'avatars/11111111-2222-4333-8444-555555555555.jpg' WHERE id = ?", user);
        MockHttpServletResponse login = login(email);
        assertThat(json.readTree(login.getContentAsString()).get("account").get("avatarUrl").asText())
                .isEqualTo("/api/v1/media/avatars/11111111-2222-4333-8444-555555555555.jpg");

        Matcher cookie = COOKIE_VALUE.matcher(login.getHeader("Set-Cookie"));
        assertThat(cookie.find()).isTrue();
        mvc.perform(post("/api/v1/auth/refresh").header("X-WikiCook-Client", "web").cookie(new Cookie("wc_rt", cookie.group(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.avatarUrl", matchesPattern("/api/v1/media/avatars/1111.*\\.jpg")));
    }

    private MockHttpServletResponse login(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"%s\",\"password\":\"Correct-horse-1\",\"rememberMe\":false}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse();
    }
}
