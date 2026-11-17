package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** T053: {@code GET}, {@code POST /me/profile} without a picture (US2-1, US2-2, Edge: Bio, Edge: Two tabs). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class MyProfileControllerIT extends AbstractPostgresIT {

    private static final String URL = "/api/v1/me/profile";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    long user;

    @BeforeEach
    void setUp() {
        user = support.member();
    }

    static String json(String displayName, String bio, boolean removeAvatar, int version) {
        String bioJson = bio == null ? "null" : "\"" + bio.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        return "{\"displayName\":\"%s\",\"bio\":%s,\"removeAvatar\":%s,\"version\":%d}".formatted(
                displayName.replace("\"", "\\\""), bioJson, removeAvatar, version);
    }

    ResultActions post(long who, String profileJson) throws Exception {
        return mvc.perform(multipart(URL)
                .file(new MockMultipartFile("profile", "", "application/json", profileJson.getBytes(StandardCharsets.UTF_8)))
                .header(HeaderCurrentUser.HEADER, who));
    }

    private ResultActions post(String profileJson) throws Exception {
        return post(user, profileJson);
    }

    private Map<String, Object> row(long id) {
        return jdbc.queryForMap("SELECT display_name, bio, profile_version FROM users WHERE id = ?", id);
    }

    @Test
    @DisplayName("FR-001: the profile shows the name, the bio, the version and the account data of the caller")
    void getMyProfile() throws Exception {
        jdbc.update("UPDATE users SET display_name = 'Hiền', bio = 'Thích nấu ăn' WHERE id = ?", user);

        mvc.perform(get(URL).header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user))
                .andExpect(jsonPath("$.displayName").value("Hiền"))
                .andExpect(jsonPath("$.bio").value("Thích nấu ăn"))
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.account.email").isString())
                .andExpect(jsonPath("$.account.role").value("MEMBER"))
                .andExpect(jsonPath("$.account.status").value("ACTIVE"))
                .andExpect(jsonPath("$.account.emailVerified").value(true))
                .andExpect(jsonPath("$.account.hasPassword").value(false))
                .andExpect(jsonPath("$.account.googleLinked").value(false))
                .andExpect(jsonPath("$.account.createdAt").isString());
    }

    @Test
    @DisplayName("NFR-004: the profile never carries the password hash, the Google subject or a dietary value")
    void getMyProfileHasNoSecrets() throws Exception {
        long withPassword = support.memberWithPassword(AuthTestSupport.uniqueEmail(), "Correct-Horse-9");
        jdbc.update("UPDATE users SET google_sub = 'google-sub-123', household_size = 7, diet_type = 'VEGAN' WHERE id = ?", withPassword);

        String body = mvc.perform(get(URL).header(HeaderCurrentUser.HEADER, withPassword)).andExpect(status().isOk())
                .andExpect(jsonPath("$.account.hasPassword").value(true))
                .andExpect(jsonPath("$.account.googleLinked").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("$2a$").doesNotContain("$2b$").doesNotContain("google-sub-123")
                .doesNotContainIgnoringCase("passwordHash").doesNotContainIgnoringCase("password_hash")
                .doesNotContain("householdSize").doesNotContain("VEGAN").doesNotContain("dietType");
    }

    @Test
    @DisplayName("US2-1: a name of 1 to 50 characters and a bio are saved and come back")
    void saveNameAndBio() throws Exception {
        post(json("H", "Bio một dòng", false, 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("H"))
                .andExpect(jsonPath("$.bio").value("Bio một dòng"))
                .andExpect(jsonPath("$.version").value(1));
        String fifty = "Ng".repeat(25);

        post(json(fifty, null, false, 1)).andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value(fifty))
                .andExpect(jsonPath("$.bio").value(nullValue())).andExpect(jsonPath("$.version").value(2));

        assertThat(row(user)).containsEntry("display_name", fifty).containsEntry("profile_version", 2);
    }

    @Test
    @DisplayName("Edge: Display name - spaces around the name are removed, 50 emoji are 50 characters")
    void nameIsStrippedAndCountedInCodePoints() throws Exception {
        post(json("  Bếp Lan  ", null, false, 0)).andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Bếp Lan"));

        post(json("😀".repeat(50), null, false, 1)).andExpect(status().isOk());
        assertThat(((String) row(user).get("display_name")).codePointCount(0, ((String) row(user).get("display_name")).length()))
                .isEqualTo(50);
    }

    @Test
    @DisplayName("US2-2: an empty name, spaces only, 51 characters, or a control character is 400 beside displayName, nothing saved")
    void badNamesAreRefused() throws Exception {
        String before = (String) row(user).get("display_name");
        for (String bad : new String[] {"", "   ", "x".repeat(51), "😀".repeat(51), "Hi\\nthere", "Hi\\u0000"}) {
            post("{\"displayName\":\"%s\",\"bio\":null,\"removeAvatar\":false,\"version\":0}".formatted(bad))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors[0].field").value("displayName"));
        }
        assertThat(row(user)).containsEntry("display_name", before).containsEntry("profile_version", 0);
    }

    @Test
    @DisplayName("US2-2: the error codes say what is wrong with the name")
    void nameErrorCodes() throws Exception {
        post(json("", null, false, 0)).andExpect(jsonPath("$.errors[*].code").value(org.hamcrest.Matchers.hasItem("DISPLAY_NAME_BLANK")));
        post(json("x".repeat(51), null, false, 0)).andExpect(jsonPath("$.errors[0].code").value("DISPLAY_NAME_TOO_LONG"));
        post(json("Hi\\u0007", null, false, 0)).andExpect(jsonPath("$.errors[0].code").value("DISPLAY_NAME_INVALID"));
    }

    @Test
    @DisplayName("Edge: Bio - 300 characters are saved with their line breaks, 301 is 400 BIO_TOO_LONG")
    void bioLimitAndLineBreaks() throws Exception {
        String multiline = "Dòng một\n\nDòng hai\nDòng ba";
        post(json("Hiền", multiline, false, 0)).andExpect(status().isOk()).andExpect(jsonPath("$.bio").value(multiline));
        assertThat(row(user).get("bio")).isEqualTo(multiline);

        post(json("Hiền", "a".repeat(300), false, 1)).andExpect(status().isOk());
        post(json("Hiền", "😀".repeat(300), false, 2)).andExpect(status().isOk());
        post(json("Hiền", "a".repeat(301), false, 3)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("bio"))
                .andExpect(jsonPath("$.errors[0].code").value("BIO_TOO_LONG"));
        assertThat(row(user).get("bio")).isEqualTo("😀".repeat(300));
    }

    @Test
    @DisplayName("Edge: Bio - a bio with a control character other than a line break is 400 BIO_INVALID")
    void bioWithControlCharacters() throws Exception {
        post("{\"displayName\":\"Hiền\",\"bio\":\"x\\u0000y\",\"removeAvatar\":false,\"version\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("bio"))
                .andExpect(jsonPath("$.errors[0].code").value("BIO_INVALID"));
    }

    @Test
    @DisplayName("Edge: Bio - clearing the bio is allowed and stores NULL")
    void clearBio() throws Exception {
        post(json("Hiền", "tạm thời", false, 0)).andExpect(status().isOk());

        post(json("Hiền", "", false, 1)).andExpect(status().isOk()).andExpect(jsonPath("$.bio").value(nullValue()));
        assertThat(row(user).get("bio")).isNull();
    }

    @Test
    @DisplayName("Assumptions: the email is read-only - a request that sends one does not change it")
    void emailCanNotBeChanged() throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, user);

        post("{\"displayName\":\"Hiền\",\"email\":\"attacker@evil.test\",\"role\":\"ADMINISTRATOR\",\"bio\":null,"
                + "\"removeAvatar\":false,\"version\":0}").andExpect(status().isOk())
                .andExpect(jsonPath("$.account.email").value(email))
                .andExpect(jsonPath("$.account.role").value("MEMBER"));

        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, user)).isEqualTo(email);
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, user)).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("Edge: Two tabs - the tab with the old version gets 409 PROFILE_CHANGED and its content is not saved")
    void staleTabIsToldAndSavingOverItWins() throws Exception {
        post(json("Tab A", null, false, 0)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));

        post(json("Tab B", null, false, 0)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PROFILE_CHANGED"));
        assertThat(row(user)).containsEntry("display_name", "Tab A");

        post(json("Tab B", null, false, 1)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
        assertThat(row(user)).containsEntry("display_name", "Tab B");
    }

    @Test
    @DisplayName("Edge: Repeated clicks - the same form twice is 200 twice and one version step")
    void repeatedClicks() throws Exception {
        post(json("Bếp Lan", "xin chào", false, 0)).andExpect(status().isOk());
        post(json("Bếp Lan", "xin chào", false, 0)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));

        assertThat(row(user)).containsEntry("profile_version", 1);
    }

    @Test
    @DisplayName("FR-006: only the caller's own row is read and written")
    void onlyOwnRow() throws Exception {
        long other = support.member();
        String otherName = (String) row(other).get("display_name");

        post(json("Chỉ của tôi", null, false, 0)).andExpect(status().isOk());

        assertThat(row(other)).containsEntry("display_name", otherName);
        mvc.perform(get(URL).header(HeaderCurrentUser.HEADER, other)).andExpect(jsonPath("$.displayName").value(otherName))
                .andExpect(jsonPath("$.displayName").value(not(containsString("Chỉ của tôi"))));
    }

    @Test
    @DisplayName("Edge: Invalid data - a body that is missing, not JSON, or lacks the version is 400")
    void malformedRequests() throws Exception {
        mvc.perform(multipart(URL).header(HeaderCurrentUser.HEADER, user)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        post("not json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        post("{\"displayName\":\"Hiền\",\"bio\":null,\"removeAvatar\":false}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("version"));
    }

    @Test
    @DisplayName("FR-006: without a sign-in both routes are 401 UNAUTHENTICATED")
    void withoutSignIn() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(multipart(URL).file(new MockMultipartFile("profile", "", "application/json",
                        json("Hiền", null, false, 0).getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isUnauthorized());
    }
}
