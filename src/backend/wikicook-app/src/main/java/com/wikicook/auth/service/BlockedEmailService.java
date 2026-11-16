package com.wikicook.auth.service;

import com.wikicook.auth.api.BlockedEmails;
import com.wikicook.auth.domain.BlockedEmail;
import com.wikicook.auth.repository.BlockedEmailRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link BlockedEmails} on {@code blocked_emails}: the table has no email in clear and no foreign key (FR-026). */
@Service
public class BlockedEmailService implements BlockedEmails {

    private final BlockedEmailRepository repository;
    private final EmailHasher hasher;
    private final Clock clock;

    public BlockedEmailService(BlockedEmailRepository repository, EmailHasher hasher, Clock clock) {
        this.repository = repository;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String email) {
        return repository.existsById(hasher.hash(email));
    }

    @Override
    @Transactional
    public void block(String email) {
        String hash = hasher.hash(email);
        if (!repository.existsById(hash)) {
            repository.save(new BlockedEmail(hash, clock.instant()));
        }
    }
}
