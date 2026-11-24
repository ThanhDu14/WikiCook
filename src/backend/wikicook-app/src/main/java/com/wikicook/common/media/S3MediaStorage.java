package com.wikicook.common.media;

import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * {@link MediaStorage} on an S3 bucket. The key is the object key, so the database keeps only the path on S3
 * ({@code avatars/<uuid>.jpg}, Cooksnap keys under {@code cooksnaps/}). The bucket stays private: the app reaches it
 * with the default AWS credentials chain (IAM role on AWS, otherwise the {@code AWS_*} environment variables), and the
 * public reaches it through CloudFront (Origin Access Control) or through the media routes of the backend.
 *
 * <p>A single PUT is atomic on S3, so a reader sees the old object or the new one, as with the disk store.
 */
public class S3MediaStorage implements MediaStorage {

    /** A new avatar is a new key, so an avatar object never changes. */
    static final String CACHE_FOR_A_YEAR = "public, max-age=31536000, immutable";
    /** Short enough that a Cooksnap taken off the public paths leaves the CDN caches within the hour. */
    static final String CACHE_FOR_AN_HOUR = "public, max-age=3600";

    private final S3Client s3;
    private final String bucket;

    public S3MediaStorage(S3Client s3, String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("wikicook.media.s3.bucket is required when wikicook.media.storage=s3");
        }
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void store(String key, byte[] content) {
        MediaKeys.requireValid(key);
        s3.putObject(request -> request
                        .bucket(bucket)
                        .key(key)
                        .contentType(MediaKeys.contentType(key))
                        .cacheControl(key.startsWith("avatars/") ? CACHE_FOR_A_YEAR : CACHE_FOR_AN_HOUR),
                RequestBody.fromBytes(content));
    }

    @Override
    public Optional<Resource> load(String key) {
        MediaKeys.requireValid(key);
        try {
            ResponseBytes<GetObjectResponse> bytes = s3.getObjectAsBytes(request -> request.bucket(bucket).key(key));
            return Optional.of(new ByteArrayResource(bytes.asByteArray(), key));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        MediaKeys.requireValid(key);
        // DeleteObject succeeds for a key that is not there
        s3.deleteObject(request -> request.bucket(bucket).key(key));
    }

    @Override
    public void move(String from, String to) {
        MediaKeys.requireValid(from);
        MediaKeys.requireValid(to);
        try {
            s3.copyObject(request -> request
                    .sourceBucket(bucket).sourceKey(from)
                    .destinationBucket(bucket).destinationKey(to));
        } catch (NoSuchKeyException e) {
            return;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return;
            }
            throw e;
        }
        s3.deleteObject(request -> request.bucket(bucket).key(from));
    }
}
