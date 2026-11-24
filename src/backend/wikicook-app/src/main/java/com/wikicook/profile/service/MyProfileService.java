package com.wikicook.profile.service;

import com.wikicook.common.media.MediaUrls;
import com.wikicook.common.media.MediaStorage;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.profile.domain.ProfileWriteKind;
import com.wikicook.profile.domain.UserProfile;
import com.wikicook.profile.dto.AccountInfoDto;
import com.wikicook.profile.dto.MyProfileDto;
import com.wikicook.profile.dto.ProfileUpdateRequest;
import com.wikicook.profile.port.ProfileAuditPort;
import com.wikicook.profile.repository.UserProfileRepository;
import com.wikicook.profile.repository.UserProfileRepository.AccountRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The public profile of the signed-in person: name, bio and avatar (US2, research R7, R12, R13, R14). The picture is
 * processed outside any transaction and written under a new key first; the transaction then changes the row, and the
 * files follow the outcome: the old file goes after the commit, the new one goes after a rollback. Nothing overwrites a
 * file, so a failure at any step leaves the old picture in place. Takes the person's id from the caller only (FR-006).
 */
@Service
public class MyProfileService {

    private static final Logger log = LoggerFactory.getLogger(MyProfileService.class);
    static final String AUDIT_ACTION = "PROFILE_UPDATED";

    private final UserProfileRepository profiles;
    private final AvatarImageProcessor processor;
    private final MediaStorage storage;
    private final ProfileWriteLimiter limiter;
    private final ProfileAuditPort audit;
    private final TransactionTemplate tx;

    public MyProfileService(UserProfileRepository profiles, AvatarImageProcessor processor, MediaStorage storage,
            ProfileWriteLimiter limiter, ProfileAuditPort audit, TransactionTemplate tx) {
        this.profiles = profiles;
        this.processor = processor;
        this.storage = storage;
        this.limiter = limiter;
        this.audit = audit;
        this.tx = tx;
    }

    public MyProfileDto get(long userId) {
        UserProfile profile = profiles.findById(userId).orElseThrow(() -> new ProfileException(ProfileErrorCode.USER_NOT_FOUND));
        return toDto(profile, userId);
    }

    /**
     * @param avatar the uploaded bytes, {@code null} when the form carries no new picture
     * @return the profile after the save, or as it is when nothing changed (FR-041)
     */
    public MyProfileDto save(long userId, ProfileUpdateRequest request, byte[] avatar) {
        // NFR-008: counted in its own transaction and before the decode, so a fake picture still spends the turn
        limiter.acquire(userId, avatar != null ? ProfileWriteKind.AVATAR_UPLOAD : ProfileWriteKind.PROFILE_SAVE);
        String newKey = null;
        if (avatar != null) {
            byte[] processed = processor.process(avatar);
            newKey = "avatars/" + UUID.randomUUID() + ".jpg";
            storage.store(newKey, processed);
        }
        String stored = newKey;
        tx.executeWithoutResult(status -> change(userId, request, stored));
        return get(userId);
    }

    private void change(long userId, ProfileUpdateRequest request, String newKey) {
        if (newKey != null) {
            removeNewFileUnlessCommitted(newKey); // registered first: every failure below ends in a rollback
        }
        UserProfile profile = profiles.findById(userId).orElseThrow(() -> new ProfileException(ProfileErrorCode.USER_NOT_FOUND));
        boolean nameChanged = !request.displayName().equals(profile.getDisplayName());
        boolean bioChanged = !Objects.equals(request.bio(), profile.getBio());
        boolean removed = newKey == null && request.removeAvatar() && profile.getAvatarStorageKey() != null;
        boolean avatarChanged = newKey != null || removed;
        if (!nameChanged && !bioChanged && !avatarChanged) {
            return; // FR-041: the same form again is not a change, whatever its version
        }
        if (!Objects.equals(request.version(), profile.getProfileVersion())) {
            throw new ProfileException(ProfileErrorCode.PROFILE_CHANGED);
        }
        String oldKey = profile.getAvatarStorageKey();
        profile.setDisplayName(request.displayName());
        profile.setBio(request.bio());
        if (avatarChanged) {
            profile.setAvatarStorageKey(newKey); // null when the picture is removed
        }
        profiles.saveAndFlush(profile); // the @Version check happens here, inside the transaction
        List<String> fields = new ArrayList<>();
        if (nameChanged) {
            fields.add("displayName");
        }
        if (bioChanged) {
            fields.add("bio");
        }
        if (avatarChanged) {
            fields.add("avatar");
        }
        audit.record(userId, AUDIT_ACTION, Map.of("fields", fields)); // names of fields only, never a value (R14)
        if (avatarChanged && oldKey != null) {
            removeOldFileAfterCommit(oldKey);
        }
    }

    private void removeNewFileUnlessCommitted(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(key);
                }
            }
        });
    }

    private void removeOldFileAfterCommit(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(key);
            }
        });
    }

    /** A file that could not be removed is logged and left: the save itself has already been decided. */
    private void deleteQuietly(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            log.error("Could not delete avatar file {}", key, e);
        }
    }

    private MyProfileDto toDto(UserProfile profile, long userId) {
        AccountRow account = profiles.findAccount(userId).orElseThrow(() -> new ProfileException(ProfileErrorCode.USER_NOT_FOUND));
        AccountInfoDto info = new AccountInfoDto(account.getEmail(), Role.valueOf(account.getRole()),
                AccountStatus.valueOf(account.getStatus()), account.getEmailVerified(), account.getHasPassword(),
                account.getGoogleLinked(), account.getCreatedAt());
        return new MyProfileDto(profile.getId(), profile.getDisplayName(), profile.getBio(),
                MediaUrls.of(profile.getAvatarStorageKey()), profile.getProfileVersion(), info);
    }
}
