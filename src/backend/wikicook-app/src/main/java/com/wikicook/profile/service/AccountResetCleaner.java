package com.wikicook.profile.service;

import com.wikicook.auth.api.AccountResetEvent;
import com.wikicook.common.media.MediaStorage;
import com.wikicook.profile.domain.UserProfile;
import com.wikicook.profile.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 011's part of the takeover of an unverified account by Google sign-in (spec 010 FR-019, Integration Points). The
 * dietary profile, bio and name go with the {@code users} row and its {@code ON DELETE CASCADE} tables; what does not
 * is the avatar <em>file</em>. It is removed after the commit of the takeover, so a rollback keeps the picture of the
 * account that stays. A file that cannot be removed is logged and left, as in {@link MyProfileService}: the takeover
 * itself is already decided.
 */
@Component
class AccountResetCleaner {

    private static final Logger log = LoggerFactory.getLogger(AccountResetCleaner.class);

    private final UserProfileRepository profiles;
    private final MediaStorage storage;

    AccountResetCleaner(UserProfileRepository profiles, MediaStorage storage) {
        this.profiles = profiles;
        this.storage = storage;
    }

    @EventListener
    void onAccountReset(AccountResetEvent event) {
        String key = profiles.findById(event.userId()).map(UserProfile::getAvatarStorageKey).orElse(null);
        if (key == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storage.delete(key);
                } catch (RuntimeException e) {
                    log.error("Could not delete avatar file {} of a reset account", key, e);
                }
            }
        });
    }
}
