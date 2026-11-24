package com.wikicook.common.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** The S3 store over a mocked client: right bucket, key, MIME type and cache policy; missing objects are empty. */
@SuppressWarnings("unchecked")
class S3MediaStorageTest {

    S3Client s3;
    S3MediaStorage storage;

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        storage = new S3MediaStorage(s3, "wikicook-media");
    }

    private static <B, R> R built(Consumer<B> consumer, B builder, java.util.function.Function<B, R> build) {
        consumer.accept(builder);
        return build.apply(builder);
    }

    @Test
    @DisplayName("store puts the object with its MIME type; avatars are cached for a year, Cooksnaps for an hour")
    void store() throws IOException {
        storage.store("avatars/a.jpg", new byte[] {1, 2});
        storage.store("cooksnaps/cs_1.webp", new byte[] {3});

        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> request = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3, org.mockito.Mockito.times(2)).putObject(request.capture(), body.capture());

        PutObjectRequest avatar = built(request.getAllValues().get(0), PutObjectRequest.builder(), PutObjectRequest.Builder::build);
        assertThat(avatar.bucket()).isEqualTo("wikicook-media");
        assertThat(avatar.key()).isEqualTo("avatars/a.jpg");
        assertThat(avatar.contentType()).isEqualTo("image/jpeg");
        assertThat(avatar.cacheControl()).isEqualTo(S3MediaStorage.CACHE_FOR_A_YEAR);
        assertThat(body.getAllValues().get(0).contentStreamProvider().newStream().readAllBytes()).containsExactly(1, 2);

        PutObjectRequest cooksnap = built(request.getAllValues().get(1), PutObjectRequest.builder(), PutObjectRequest.Builder::build);
        assertThat(cooksnap.contentType()).isEqualTo("image/webp");
        assertThat(cooksnap.cacheControl()).isEqualTo(S3MediaStorage.CACHE_FOR_AN_HOUR);
    }

    @Test
    @DisplayName("load returns the bytes of the object, and empty when there is no such key")
    void load() throws IOException {
        when(s3.getObjectAsBytes(any(Consumer.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[] {7, 8}))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        Optional<Resource> found = storage.load("avatars/a.jpg");
        assertThat(found).isPresent();
        assertThat(found.get().getContentAsByteArray()).containsExactly(7, 8);
        assertThat(found.get().contentLength()).isEqualTo(2);

        assertThat(storage.load("avatars/missing.jpg")).isEmpty();
    }

    @Test
    @DisplayName("delete removes the object from the bucket")
    void delete() {
        storage.delete("avatars/a.jpg");

        ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> request = ArgumentCaptor.forClass(Consumer.class);
        verify(s3).deleteObject(request.capture());
        DeleteObjectRequest sent = built(request.getValue(), DeleteObjectRequest.builder(), DeleteObjectRequest.Builder::build);
        assertThat(sent.bucket()).isEqualTo("wikicook-media");
        assertThat(sent.key()).isEqualTo("avatars/a.jpg");
    }

    @Test
    @DisplayName("move copies inside the bucket then deletes the source; a missing source is not an error")
    void move() {
        storage.move("cooksnaps/cs_1.webp", "hidden/cooksnaps/cs_1.webp");

        ArgumentCaptor<Consumer<CopyObjectRequest.Builder>> copy = ArgumentCaptor.forClass(Consumer.class);
        verify(s3).copyObject(copy.capture());
        CopyObjectRequest sent = built(copy.getValue(), CopyObjectRequest.builder(), CopyObjectRequest.Builder::build);
        assertThat(sent.sourceKey()).isEqualTo("cooksnaps/cs_1.webp");
        assertThat(sent.destinationKey()).isEqualTo("hidden/cooksnaps/cs_1.webp");
        verify(s3).deleteObject(any(Consumer.class));

        S3Client empty = mock(S3Client.class);
        when(empty.copyObject(any(Consumer.class))).thenThrow(NoSuchKeyException.builder().message("missing").build());
        new S3MediaStorage(empty, "wikicook-media").move("cooksnaps/gone.webp", "hidden/cooksnaps/gone.webp");
        verify(empty, never()).deleteObject(any(Consumer.class));
    }

    @Test
    @DisplayName("keys that are not plain relative paths never reach S3; a blank bucket is refused")
    void refusals() {
        assertThatThrownBy(() -> storage.store("../x.jpg", new byte[] {1})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.load("/etc/passwd")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.delete("")).isInstanceOf(IllegalArgumentException.class);
        verify(s3, never()).putObject(any(Consumer.class), any(RequestBody.class));

        assertThatThrownBy(() -> new S3MediaStorage(s3, " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
