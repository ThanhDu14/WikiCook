package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.auth.domain.BlockedEmail;
import com.wikicook.auth.repository.BlockedEmailRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** FR-026: the Blocked Email list holds only the HMAC of the normalized email. */
class BlockedEmailServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T03:00:00Z");

    private final BlockedEmailRepository repository = mock(BlockedEmailRepository.class);
    private final EmailHasher hasher = new EmailHasher("unit-test-hash-key-0123456789");
    private final BlockedEmailService service = new BlockedEmailService(repository, hasher,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void blockStoresOnlyTheHashOfTheNormalizedEmail() {
        service.block("  An@Mail.COM ");

        ArgumentCaptor<BlockedEmail> saved = ArgumentCaptor.forClass(BlockedEmail.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getEmailHash()).isEqualTo(hasher.hash("an@mail.com"));
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void blockingAnAlreadyBlockedEmailChangesNothing() {
        when(repository.existsById(hasher.hash("an@mail.com"))).thenReturn(true);

        service.block("an@mail.com");

        verify(repository, never()).save(any());
    }

    @Test
    void isBlockedLooksUpTheNormalizedHash() {
        when(repository.existsById(hasher.hash("an@mail.com"))).thenReturn(true);

        assertThat(service.isBlocked(" AN@mail.com ")).isTrue();
        assertThat(service.isBlocked("other@mail.com")).isFalse();
    }
}
