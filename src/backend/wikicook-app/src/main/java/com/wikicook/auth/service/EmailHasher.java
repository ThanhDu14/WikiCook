package com.wikicook.auth.service;

import com.wikicook.auth.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 of the normalized email with {@code WIKICOOK_EMAIL_HASH_KEY}, as 64 hex characters. Used by
 * {@code login_attempts}, {@code email_send_requests} and {@code blocked_emails}, so none of them stores an email
 * (research R4, R11). Changing the key after data exists makes every stored hash stop matching.
 */
@Component
public class EmailHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    @Autowired
    public EmailHasher(AuthProperties properties) {
        this(properties.emailHashKey());
    }

    public EmailHasher(String key) {
        this.key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** Normalizes first, so {@code " An@Mail.com "} and {@code "an@mail.com"} give the same hash. */
    public String hash(String email) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal(EmailNormalizer.normalize(email).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
