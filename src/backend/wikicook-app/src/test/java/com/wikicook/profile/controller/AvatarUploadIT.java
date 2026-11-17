package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.common.media.MediaStorage;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * T055 (US2-3, US2-5, SC-003 on the server, NFR-004): the picture goes through {@code POST /me/profile} and comes back
 * from {@code GET /media/avatars/{file}}. Runs on a real port too, because the 5 MB multipart limit is enforced by
 * Tomcat and MockMvc does not have it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class AvatarUploadIT extends AbstractPostgresIT {

    private static final String PROFILE = "/api/v1/me/profile";
    private static final String UUID_FILE = "avatars/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg";

    @LocalServerPort
    int port;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    MediaStorage storage;

    @Autowired
    ObjectMapper json;

    @Value("${wikicook.profile.media-dir}")
    String mediaDir;

    long user;

    @BeforeEach
    void setUp() {
        user = support.member();
    }

    /** A 800 x 800 picture of random pixels: a PNG of about 2 MB that does not compress. */
    private static byte[] noisyPng() throws IOException {
        Random random = new Random(42);
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 800; x++) {
            for (int y = 0; y < 800; y++) {
                image.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] sample(String name) throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/avatars", name));
    }

    private ResultActions upload(long who, String name, String bio, boolean removeAvatar, int version, String fileName,
            String contentType, byte[] content) throws Exception {
        String profile = MyProfileControllerIT.json(name, bio, removeAvatar, version);
        MockMultipartHttpServletRequestBuilder request = multipart(PROFILE)
                .file(new MockMultipartFile("profile", "", "application/json", profile.getBytes(StandardCharsets.UTF_8)));
        if (content != null) {
            request.file(new MockMultipartFile("avatar", fileName, contentType, content));
        }
        request.header(HeaderCurrentUser.HEADER, who);
        return mvc.perform(request);
    }

    private String storedKey(long who) {
        return jdbc.queryForObject("SELECT avatar_storage_key FROM users WHERE id = ?", String.class, who);
    }

    private long filesStored() throws IOException {
        Path avatars = Path.of(mediaDir).resolve("avatars");
        if (!Files.isDirectory(avatars)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(avatars)) {
            return files.count();
        }
    }

    private BufferedImage fetch(String url) throws Exception {
        MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        return ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
    }

    @Test
    @DisplayName("US2-3: a 2 MB PNG is saved with the profile, shown as a 512 x 512 JPEG from the media route")
    void uploadAPng() throws Exception {
        byte[] png = noisyPng();
        assertThat(png.length).as("the sample is about 2 MB").isGreaterThan(1_500_000).isLessThan(5_000_000);

        MvcResult result = upload(user, "Hiền", "Thích nấu ăn", false, 0, "avatar.png", "image/png", png)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(matchesPattern("/api/v1/media/" + UUID_FILE)))
                .andExpect(jsonPath("$.displayName").value("Hiền"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();
        String url = json.readTree(result.getResponse().getContentAsString()).get("avatarUrl").asText();

        assertThat(storedKey(user)).matches(UUID_FILE);
        assertThat(storage.load(storedKey(user))).isPresent();
        BufferedImage served = fetch(url);
        assertThat(served.getWidth()).isEqualTo(512);
        assertThat(served.getHeight()).isEqualTo(512);
    }

    @Test
    @DisplayName("NFR-004: the media route says image/jpeg, nosniff, inline, and caches for a year (the key changes with the picture)")
    void mediaRouteHeaders() throws Exception {
        upload(user, "Hiền", null, false, 0, "a.jpg", "image/jpeg", sample("plain.jpg")).andExpect(status().isOk());
        String url = "/api/v1/media/" + storedKey(user);

        mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", "inline"))
                .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"));
    }

    @Test
    @DisplayName("NFR-004: file names that do not look like a stored avatar are 404, none of them reads another path")
    void mediaRouteRejectsOtherNames() throws Exception {
        upload(user, "Hiền", null, false, 0, "a.jpg", "image/jpeg", sample("plain.jpg")).andExpect(status().isOk());
        String good = storedKey(user).substring("avatars/".length());

        for (String name : List.of("abc.jpg", good.toUpperCase(), good.replace(".jpg", ".png"), good + ".exe",
                "00000000-0000-0000-0000-000000000000.jpg", "%2e%2e%2fapplication.yml", "..%2F..%2Fetc%2Fpasswd",
                "x/" + good, good.substring(1))) {
            mvc.perform(get("/api/v1/media/avatars/" + name)).andExpect(status().is4xxClientError());
        }
        mvc.perform(get("/api/v1/media/avatars/abc.jpg")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/media/avatars/00000000-0000-0000-0000-000000000000.jpg")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US2-5: a second picture replaces the first: new key, the old file is gone after the commit")
    void replacingThePicture() throws Exception {
        upload(user, "Hiền", null, false, 0, "a.png", "image/png", sample("alpha.png")).andExpect(status().isOk());
        String first = storedKey(user);

        upload(user, "Hiền", null, false, 1, "b.webp", "image/webp", sample("lossy.webp")).andExpect(status().isOk());
        String second = storedKey(user);

        assertThat(second).isNotEqualTo(first);
        assertThat(storage.load(first)).as("the old file").isEmpty();
        assertThat(storage.load(second)).as("the new file").isPresent();
        mvc.perform(get("/api/v1/media/" + first)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US2-5: removing the picture returns the default avatar and deletes the file")
    void removingThePicture() throws Exception {
        upload(user, "Hiền", null, false, 0, "a.png", "image/png", sample("alpha.png")).andExpect(status().isOk());
        String key = storedKey(user);

        upload(user, "Hiền", null, true, 1, null, null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()));

        assertThat(storedKey(user)).isNull();
        assertThat(storage.load(key)).isEmpty();
    }

    @Test
    @DisplayName("Edge: Avatar not an image / SC-003: every file that is not a JPEG, PNG or WebP picture is refused, nothing is saved")
    void nonImagesAreRefused() throws Exception {
        byte[] garbageJpeg = new byte[300];
        garbageJpeg[0] = (byte) 0xFF;
        garbageJpeg[1] = (byte) 0xD8;
        garbageJpeg[2] = (byte) 0xFF;
        Object[][] cases = {
                {"photo.png", "image/png", "just some text".getBytes(StandardCharsets.UTF_8), 415, "AVATAR_TYPE_NOT_ALLOWED"},
                {"virus.png", "image/png", new byte[] {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0}, 415, "AVATAR_TYPE_NOT_ALLOWED"},
                {"anim.gif", "image/gif", "GIF89a\u0001\u0000\u0001\u0000".getBytes(StandardCharsets.ISO_8859_1), 415, "AVATAR_TYPE_NOT_ALLOWED"},
                {"logo.svg", "image/svg+xml", "<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8),
                        415, "AVATAR_TYPE_NOT_ALLOWED"},
                {"doc.png", "image/png", "%PDF-1.7".getBytes(StandardCharsets.UTF_8), 415, "AVATAR_TYPE_NOT_ALLOWED"},
                {"empty.png", "image/png", new byte[0], 415, "AVATAR_TYPE_NOT_ALLOWED"},
                {"fake.jpg", "image/jpeg", garbageJpeg, 422, "AVATAR_NOT_AN_IMAGE"},
                {"cut.jpg", "image/jpeg", java.util.Arrays.copyOf(sample("plain.jpg"), 400), 422, "AVATAR_NOT_AN_IMAGE"},
        };
        long filesBefore = filesStored();

        for (Object[] c : cases) {
            upload(user, "Tên mới", null, false, 0, (String) c[0], (String) c[1], (byte[]) c[2])
                    .andExpect(status().is((Integer) c[3]))
                    .andExpect(jsonPath("$.code").value((String) c[4]));
        }

        assertThat(storedKey(user)).isNull();
        assertThat(jdbc.queryForObject("SELECT display_name FROM users WHERE id = ?", String.class, user)).isNotEqualTo("Tên mới");
        assertThat(filesStored()).isEqualTo(filesBefore);
    }

    @Test
    @DisplayName("R7: more than 6000 pixels on a side is 422 AVATAR_DIMENSIONS_TOO_LARGE")
    void tooManyPixels() throws Exception {
        BufferedImage wide = new BufferedImage(6001, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(wide, "png", out);

        upload(user, "Hiền", null, false, 0, "wide.png", "image/png", out.toByteArray())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("AVATAR_DIMENSIONS_TOO_LARGE"));
    }

    @Test
    @DisplayName("NFR-004: the stored file is a re-encoded JPEG without the EXIF of the upload")
    void storedFileHasNoExif() throws Exception {
        upload(user, "Hiền", null, false, 0, "gps.jpg", "image/jpeg", sample("gps.jpg")).andExpect(status().isOk());

        byte[] stored = storage.load(storedKey(user)).orElseThrow().getContentAsByteArray();

        assertThat(new String(stored, StandardCharsets.ISO_8859_1)).doesNotContain("Exif").doesNotContain("GPS");
    }

    @Test
    @DisplayName("US2-3: when the database refuses the update the new file is removed and the old picture stays")
    void databaseFailureKeepsTheOldPicture() throws Exception {
        upload(user, "Hiền", null, false, 0, "a.png", "image/png", sample("alpha.png")).andExpect(status().isOk());
        String old = storedKey(user);
        long filesBefore = filesStored();
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION fail_profile_update() RETURNS trigger LANGUAGE plpgsql AS
                $$ BEGIN RAISE EXCEPTION 'simulated database failure'; END $$""");
        jdbc.execute("CREATE TRIGGER trg_fail_profile_update BEFORE UPDATE ON users FOR EACH ROW WHEN (NEW.id = " + user
                + ") EXECUTE FUNCTION fail_profile_update()");
        try {
            upload(user, "Hiền", null, false, 1, "b.png", "image/png", sample("lossless.webp")).andExpect(status().is5xxServerError());
        } finally {
            jdbc.execute("DROP TRIGGER trg_fail_profile_update ON users");
        }

        assertThat(storedKey(user)).isEqualTo(old);
        assertThat(storage.load(old)).isPresent();
        assertThat(filesStored()).as("the new file was cleaned up").isEqualTo(filesBefore);
    }

    @Test
    @DisplayName("Edge: Repeated clicks - the second upload with the old version is 409 and leaves no file behind")
    void secondUploadWithTheOldVersion() throws Exception {
        upload(user, "Hiền", null, false, 0, "a.png", "image/png", sample("alpha.png")).andExpect(status().isOk());
        long filesBefore = filesStored();

        upload(user, "Hiền", null, false, 0, "a.png", "image/png", sample("alpha.png")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFILE_CHANGED"));

        assertThat(filesStored()).isEqualTo(filesBefore);
    }

    @Test
    @DisplayName("Edge: Avatar not an image - over 5 MB on the real server is 413 AVATAR_TOO_LARGE, with a JSON body")
    void tooLargeOverHttp() throws Exception {
        byte[] almost = new byte[5 * 1024 * 1024 + 512 * 1024]; // over the 5 MB file limit, under the 6 MB request limit
        almost[0] = (byte) 0x89;
        almost[1] = 'P';
        almost[2] = 'N';
        almost[3] = 'G';
        String boundary = "----wikicook" + System.nanoTime();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"profile\"\r\nContent-Type: application/json\r\n\r\n"
                + MyProfileControllerIT.json("Hiền", null, false, 0) + "\r\n--" + boundary
                + "\r\nContent-Disposition: form-data; name=\"avatar\"; filename=\"big.png\"\r\nContent-Type: image/png\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write(almost);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        long filesBefore = filesStored();

        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + PROFILE))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header(HeaderCurrentUser.HEADER, String.valueOf(user))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(json.readTree(response.body()).get("code").asText()).isEqualTo("AVATAR_TOO_LARGE");
        assertThat(filesStored()).isEqualTo(filesBefore);
        assertThat(storedKey(user)).isNull();
    }

    @Test
    @DisplayName("US2-3: a valid picture of about 3.6 MB (random pixels do not compress) is accepted")
    void aThreeAndAHalfMegabytePictureIsFine() throws Exception {
        Random random = new Random(7);
        BufferedImage image = new BufferedImage(1100, 1100, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 1100; x++) {
            for (int y = 0; y < 1100; y++) {
                image.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        assertThat(png.size()).isBetween(3_000_000, 5_000_000);

        upload(user, "Hiền", null, false, 0, "big.png", "image/png", png.toByteArray()).andExpect(status().isOk());
    }
}
