package com.wikicook.common.media;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.review.service.MediaStorageCooksnapService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The S3 store and the Spring wiring against a real S3 API (Adobe S3Mock in Docker): the AWS SDK calls, the
 * missing-key answers and the move used to hide a Cooksnap behave as on AWS.
 */
@Testcontainers
class S3MediaStorageIT {

    private static final String BUCKET = "wikicook-media";

    @Container
    static final GenericContainer<?> S3MOCK = new GenericContainer<>("adobe/s3mock:3.12.0")
            .withEnv("initialBuckets", BUCKET)
            .withExposedPorts(9090)
            .waitingFor(Wait.forHttp("/").forPort(9090));

    @BeforeAll
    static void credentials() {
        // the default credentials chain reads these system properties; S3Mock accepts any key
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
    }

    @AfterAll
    static void clearCredentials() {
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
    }

    private ApplicationContextRunner s3Context() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MediaStorageConfig.class))
                .withPropertyValues(
                        "wikicook.media.storage=s3",
                        "wikicook.media.s3.bucket=" + BUCKET,
                        "wikicook.media.s3.region=ap-southeast-1",
                        "wikicook.media.s3.endpoint=http://" + S3MOCK.getHost() + ":" + S3MOCK.getMappedPort(9090));
    }

    @Test
    @DisplayName("storage=s3 makes the S3 store the MediaStorage; the default leaves the local disk store to 011")
    void wiring() {
        s3Context().run(context -> {
            assertThat(context).hasSingleBean(S3Client.class);
            assertThat(context.getBean(MediaStorage.class)).isInstanceOf(S3MediaStorage.class);
        });
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MediaStorageConfig.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(S3Client.class);
                    assertThat(context).doesNotHaveBean(MediaStorage.class);
                });
    }

    @Test
    @DisplayName("store, load, delete and a missing key against the S3 API")
    void storeLoadDelete() {
        s3Context().run(context -> {
            MediaStorage storage = context.getBean(MediaStorage.class);

            storage.store("avatars/0b6f-it.jpg", new byte[] {1, 2, 3});
            assertThat(storage.load("avatars/0b6f-it.jpg").orElseThrow().getContentAsByteArray()).containsExactly(1, 2, 3);

            storage.store("avatars/0b6f-it.jpg", new byte[] {4});
            assertThat(storage.load("avatars/0b6f-it.jpg").orElseThrow().getContentAsByteArray()).containsExactly(4);

            storage.delete("avatars/0b6f-it.jpg");
            storage.delete("avatars/0b6f-it.jpg");
            assertThat(storage.load("avatars/0b6f-it.jpg")).isEmpty();
        });
    }

    @Test
    @DisplayName("a Cooksnap on S3: stored under cooksnaps/, hidden under hidden/, restored, then deleted")
    void cooksnapLifecycle() {
        s3Context().run(context -> {
            MediaStorage storage = context.getBean(MediaStorage.class);
            MediaStorageCooksnapService cooksnaps = new MediaStorageCooksnapService(storage);
            String key = "cs_1_2_it.webp";

            cooksnaps.store(key, new byte[] {1, 2}, new byte[] {3});
            assertThat(storage.load("cooksnaps/cs_1_2_it.webp")).isPresent();
            assertThat(storage.load("cooksnaps/thumb_cs_1_2_it.webp")).isPresent();

            cooksnaps.registerPostCommitHide(key);
            assertThat(storage.load("cooksnaps/cs_1_2_it.webp")).isEmpty();
            assertThat(storage.load("hidden/cooksnaps/cs_1_2_it.webp")).isPresent();
            cooksnaps.registerPostCommitHide(key); // already hidden: copy of a missing key is not an error

            cooksnaps.registerPostCommitRestore(key);
            assertThat(cooksnaps.loadAsResource(key, false).getContentAsByteArray()).containsExactly(1, 2);
            assertThat(cooksnaps.loadAsResource(key, true).getContentAsByteArray()).containsExactly(3);
            assertThat(storage.load("hidden/cooksnaps/cs_1_2_it.webp")).isEmpty();

            cooksnaps.delete(key);
            assertThat(cooksnaps.loadAsResource(key, true)).isNull();
        });
    }
}
