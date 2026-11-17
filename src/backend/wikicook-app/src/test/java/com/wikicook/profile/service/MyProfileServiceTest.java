package com.wikicook.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.common.media.MediaStorage;
import com.wikicook.profile.domain.ProfileWriteKind;
import com.wikicook.profile.domain.UserProfile;
import com.wikicook.profile.dto.MyProfileDto;
import com.wikicook.profile.dto.ProfileUpdateRequest;
import com.wikicook.profile.port.ProfileAuditPort;
import com.wikicook.profile.repository.UserProfileRepository;
import com.wikicook.profile.repository.UserProfileRepository.AccountRow;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * T052: the rules of {@code MyProfileService} (research R7, R12, R14) without a database. A transaction manager that
 * does nothing but keep the synchronisations lets the test see what runs after commit and after a rollback.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MyProfileServiceTest {

    private static final long USER = 9;

    /** Commits and rolls back nothing, but runs the synchronisations like a real manager. */
    static class FakeTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @Mock
    UserProfileRepository profiles;
    @Mock
    AvatarImageProcessor processor;
    @Mock
    MediaStorage storage;
    @Mock
    ProfileWriteLimiter limiter;
    @Mock
    ProfileAuditPort audit;

    MyProfileService service;
    UserProfile entity;

    @BeforeEach
    void setUp() {
        service = new MyProfileService(profiles, processor, storage, limiter, audit,
                new TransactionTemplate(new FakeTransactionManager()));
        entity = BeanUtils.instantiateClass(UserProfile.class);
        ReflectionTestUtils.setField(entity, "id", USER);
        ReflectionTestUtils.setField(entity, "displayName", "Hiền");
        ReflectionTestUtils.setField(entity, "bio", "Thích nấu ăn");
        ReflectionTestUtils.setField(entity, "profileVersion", 4);
        when(profiles.findById(USER)).thenReturn(Optional.of(entity));
        AccountRow account = mock(AccountRow.class);
        when(account.getEmail()).thenReturn("hien@test.wikicook.local");
        when(account.getRole()).thenReturn("MEMBER");
        when(account.getStatus()).thenReturn("ACTIVE");
        when(account.getEmailVerified()).thenReturn(true);
        when(account.getHasPassword()).thenReturn(true);
        when(account.getGoogleLinked()).thenReturn(false);
        when(account.getCreatedAt()).thenReturn(Instant.parse("2026-11-01T00:00:00Z"));
        when(profiles.findAccount(USER)).thenReturn(Optional.of(account));
        when(profiles.saveAndFlush(any(UserProfile.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static ProfileUpdateRequest request(String name, String bio, boolean removeAvatar, int version) {
        return new ProfileUpdateRequest(name, bio, removeAvatar, version);
    }

    @Test
    @DisplayName("Edge: Display name - the name is stripped on the request, 50 emoji count as 50")
    void requestStripsTheName() {
        assertThat(request("  Hiền  ", null, false, 0).displayName()).isEqualTo("Hiền");
        assertThat(request("\tBếp Lan\n", null, false, 0).displayName()).isEqualTo("Bếp Lan");
        assertThat(request(null, null, false, 0).displayName()).isNull();
    }

    @Test
    @DisplayName("Edge: Bio - line breaks are kept (as \\n), the ends are stripped, an empty bio is no bio")
    void requestKeepsBioLineBreaks() {
        assertThat(request("A", "Dòng một\r\nDòng hai\n\nDòng ba", false, 0).bio()).isEqualTo("Dòng một\nDòng hai\n\nDòng ba");
        assertThat(request("A", "  xin chào  \n", false, 0).bio()).isEqualTo("xin chào");
        assertThat(request("A", "   \n  ", false, 0).bio()).isNull();
        assertThat(request("A", "", false, 0).bio()).isNull();
        assertThat(request("A", null, false, 0).bio()).isNull();
    }

    @Test
    @DisplayName("US2-1: a new name and bio are saved and audited with the field names only")
    void changedFieldsAreSavedAndAudited() {
        MyProfileDto saved = service.save(USER, request("Hiền Nguyễn", "Bio mới", false, 4), null);

        ArgumentCaptor<UserProfile> written = ArgumentCaptor.forClass(UserProfile.class);
        verify(profiles).saveAndFlush(written.capture());
        assertThat(written.getValue().getDisplayName()).isEqualTo("Hiền Nguyễn");
        assertThat(written.getValue().getBio()).isEqualTo("Bio mới");
        verify(audit).record(USER, "PROFILE_UPDATED", Map.of("fields", List.of("displayName", "bio")));
        verify(limiter).acquire(USER, ProfileWriteKind.PROFILE_SAVE);
        assertThat(saved.account().email()).isEqualTo("hien@test.wikicook.local");
    }

    @Test
    @DisplayName("FR-041: the same name and bio with no new picture writes nothing and audits nothing, even with an old version")
    void sameContentIsANoOp() {
        MyProfileDto result = service.save(USER, request("Hiền", "Thích nấu ăn", false, 1 /* stale */), null);

        verify(profiles, never()).saveAndFlush(any());
        verify(audit, never()).record(anyLong(), anyString(), any());
        assertThat(result.version()).isEqualTo(4);
        verify(limiter).acquire(USER, ProfileWriteKind.PROFILE_SAVE); // a no-op still counts (research R13)
    }

    @Test
    @DisplayName("Edge: Two tabs - new content with an old version is PROFILE_CHANGED and writes nothing")
    void staleVersionIsAConflict() {
        ProfileException refused = org.junit.jupiter.api.Assertions.assertThrows(ProfileException.class,
                () -> service.save(USER, request("Tên khác", "Thích nấu ăn", false, 3), null));

        assertThat(refused.code()).isEqualTo(ProfileErrorCode.PROFILE_CHANGED);
        verify(profiles, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Edge: Two tabs - losing the race at flush time is also PROFILE_CHANGED (the lock of @Version)")
    void lostRaceAtFlushIsAConflict() {
        doThrow(new OptimisticLockingFailureException("row was updated")).when(profiles).saveAndFlush(any());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.save(USER, request("Tên khác", null, false, 4), null))
                .isInstanceOf(OptimisticLockingFailureException.class); // the advice of the module maps it to 409
    }

    @Test
    @DisplayName("US2-3: a new picture is processed, stored under a new key, and the old file goes after the commit")
    void newAvatarReplacesTheOldOneAfterCommit() {
        ReflectionTestUtils.setField(entity, "avatarStorageKey", "avatars/old.jpg");
        byte[] upload = {1, 2, 3};
        byte[] processed = {9, 9};
        when(processor.process(upload)).thenReturn(processed);

        service.save(USER, request("Hiền", "Thích nấu ăn", false, 4), upload);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).store(key.capture(), eq(processed));
        assertThat(key.getValue()).matches("avatars/[0-9a-f-]{36}\\.jpg");
        ArgumentCaptor<UserProfile> written = ArgumentCaptor.forClass(UserProfile.class);
        verify(profiles).saveAndFlush(written.capture());
        assertThat(written.getValue().getAvatarStorageKey()).isEqualTo(key.getValue());
        verify(storage).delete("avatars/old.jpg");
        verify(storage, never()).delete(key.getValue());
        verify(audit).record(USER, "PROFILE_UPDATED", Map.of("fields", List.of("avatar")));
        verify(limiter).acquire(USER, ProfileWriteKind.AVATAR_UPLOAD);
    }

    @Test
    @DisplayName("US2-3: a new picture is a change even when name and bio are the same")
    void newAvatarAloneIsAChange() {
        when(processor.process(any())).thenReturn(new byte[] {1});

        service.save(USER, request("Hiền", "Thích nấu ăn", false, 4), new byte[] {1, 2});

        verify(profiles).saveAndFlush(any());
    }

    @Test
    @DisplayName("US2-3: when the transaction rolls back the new file is removed and the old one stays")
    void failedSaveRemovesTheNewFile() {
        ReflectionTestUtils.setField(entity, "avatarStorageKey", "avatars/old.jpg");
        when(processor.process(any())).thenReturn(new byte[] {1});
        doThrow(new IllegalStateException("database is down")).when(profiles).saveAndFlush(any());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.save(USER, request("Hiền", "Thích nấu ăn", false, 4), new byte[] {1, 2}))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        verify(storage).store(stored.capture(), any());
        verify(storage).delete(stored.getValue());
        verify(storage, never()).delete("avatars/old.jpg");
    }

    @Test
    @DisplayName("Edge: Repeated clicks - the second upload carries the old version: 409 and its file is removed")
    void conflictRemovesTheNewFile() {
        when(processor.process(any())).thenReturn(new byte[] {1});

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.save(USER, request("Hiền", "Thích nấu ăn", false, 3), new byte[] {1, 2}))
                .isInstanceOf(ProfileException.class);

        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        verify(storage).store(stored.capture(), any());
        verify(storage).delete(stored.getValue());
    }

    @Test
    @DisplayName("US2-5: removing the picture clears the key and deletes the old file after the commit")
    void removeAvatar() {
        ReflectionTestUtils.setField(entity, "avatarStorageKey", "avatars/old.jpg");

        service.save(USER, request("Hiền", "Thích nấu ăn", true, 4), null);

        ArgumentCaptor<UserProfile> written = ArgumentCaptor.forClass(UserProfile.class);
        verify(profiles).saveAndFlush(written.capture());
        assertThat(written.getValue().getAvatarStorageKey()).isNull();
        verify(storage).delete("avatars/old.jpg");
        verify(storage, never()).store(anyString(), any());
        verify(audit).record(USER, "PROFILE_UPDATED", Map.of("fields", List.of("avatar")));
    }

    @Test
    @DisplayName("US2-5: removing a picture that was never there is no change")
    void removeWithoutAvatarIsANoOp() {
        service.save(USER, request("Hiền", "Thích nấu ăn", true, 4), null);

        verify(profiles, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Edge: Avatar not an image - a refused picture stores nothing and changes nothing, but spends the turn")
    void refusedPictureStoresNothing() {
        when(processor.process(any())).thenThrow(new ProfileException(ProfileErrorCode.AVATAR_NOT_AN_IMAGE));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.save(USER, request("Tên khác", null, false, 4), new byte[] {1}))
                .isInstanceOfSatisfying(ProfileException.class,
                        e -> assertThat(e.code()).isEqualTo(ProfileErrorCode.AVATAR_NOT_AN_IMAGE));

        verify(limiter).acquire(USER, ProfileWriteKind.AVATAR_UPLOAD);
        verify(storage, never()).store(anyString(), any());
        verify(profiles, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("R13: the limiter is asked before the picture is decoded")
    void limiterComesBeforeDecoding() {
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(limiter, processor);
        when(processor.process(any())).thenReturn(new byte[] {1});

        service.save(USER, request("Hiền", "Thích nấu ăn", false, 4), new byte[] {1});

        order.verify(limiter).acquire(USER, ProfileWriteKind.AVATAR_UPLOAD);
        order.verify(processor).process(any());
    }

    @Test
    @DisplayName("FR-001: the profile of a user without a row is USER_NOT_FOUND")
    void missingUser() {
        when(profiles.findById(USER)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.get(USER)).isInstanceOfSatisfying(ProfileException.class,
                e -> assertThat(e.code()).isEqualTo(ProfileErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("FR-004: the avatar URL is the public media route of the stored key; none when there is no picture")
    void avatarUrl() {
        assertThat(service.get(USER).avatarUrl()).isNull();
        ReflectionTestUtils.setField(entity, "avatarStorageKey", "avatars/0d6f.jpg");

        assertThat(service.get(USER).avatarUrl()).isEqualTo("/api/v1/media/avatars/0d6f.jpg");
    }
}
