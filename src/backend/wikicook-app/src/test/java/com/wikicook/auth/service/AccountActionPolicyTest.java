package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.auth.service.AccountActionPolicy.Action;
import com.wikicook.auth.service.AccountActionPolicy.Party;
import com.wikicook.auth.service.AccountActionPolicy.Request;
import com.wikicook.common.security.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * FR-022, FR-022a, research R10: who may do what to whom. Only a higher role acts on a lower one (an Administrator may
 * also act on another Administrator, but then must have re-authenticated); nobody acts on themselves or on a system
 * account; every suspension is at most 30 days, for every role.
 */
class AccountActionPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-13T03:00:00Z");

    private final AccountActionPolicy policy = new AccountActionPolicy();

    private static Party party(long id, Role role) {
        return new Party(id, role, false);
    }

    private static Request suspend(Duration lasting) {
        return new Request(Action.SUSPEND, null, NOW.plus(lasting));
    }

    private static Request changeRole(Role newRole) {
        return new Request(Action.CHANGE_ROLE, newRole, null);
    }

    private static Request ban() {
        return new Request(Action.BAN, null, null);
    }

    private static Request reinstate() {
        return new Request(Action.REINSTATE, null, null);
    }

    private static Request of(Action action) {
        return switch (action) {
            case SUSPEND -> suspend(Duration.ofDays(3));
            case CHANGE_ROLE -> changeRole(Role.CONTRIBUTOR);
            case BAN -> ban();
            case REINSTATE -> reinstate();
        };
    }

    private void check(Party actor, Party target, Request request, boolean reauthenticated) {
        policy.check(actor, target, request, NOW, reauthenticated);
    }

    private static void assertCode(Throwable thrown, AuthErrorCode code) {
        assertThat(thrown).isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void nobodyActsOnThemselves(Role role) {
        for (Action action : Action.values()) {
            assertThatThrownBy(() -> check(party(1, role), party(1, role), of(action), true))
                    .as("%s %s on self", role, action)
                    .satisfies(t -> assertCode(t, AuthErrorCode.SELF_ACTION_FORBIDDEN));
        }
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void aSystemAccountIsNeverAffected(Action action) {
        Party system = new Party(9, Role.MEMBER, true);

        assertThatThrownBy(() -> check(party(1, Role.ADMINISTRATOR), system, of(action), true))
                .satisfies(t -> assertCode(t, AuthErrorCode.ACCESS_DENIED));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"MEMBER", "CONTRIBUTOR"})
    void aMemberOrContributorCanDoNothingToAnyone(Role actorRole) {
        for (Role targetRole : Role.values()) {
            for (Action action : Action.values()) {
                assertThatThrownBy(() -> check(party(1, actorRole), party(2, targetRole), of(action), true))
                        .as("%s %s %s", actorRole, action, targetRole)
                        .satisfies(t -> assertCode(t, AuthErrorCode.ACCESS_DENIED));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"MEMBER", "CONTRIBUTOR"})
    void aModeratorMaySuspendAMemberOrAContributor(Role targetRole) {
        assertThatCode(() -> check(party(1, Role.MODERATOR), party(2, targetRole), suspend(Duration.ofDays(30)), false))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"MODERATOR", "ADMINISTRATOR"})
    void aModeratorMayNotSuspendAModeratorOrAnAdministrator(Role targetRole) {
        assertThatThrownBy(() -> check(party(1, Role.MODERATOR), party(2, targetRole), suspend(Duration.ofDays(1)),
                true)).satisfies(t -> assertCode(t, AuthErrorCode.ACCESS_DENIED));
    }

    @ParameterizedTest
    @EnumSource(value = Action.class, names = {"CHANGE_ROLE", "BAN", "REINSTATE"})
    void aModeratorMayOnlySuspend(Action action) {
        for (Role targetRole : EnumSet.of(Role.MEMBER, Role.CONTRIBUTOR)) {
            assertThatThrownBy(() -> check(party(1, Role.MODERATOR), party(2, targetRole), of(action), true))
                    .as("%s on %s", action, targetRole)
                    .satisfies(t -> assertCode(t, AuthErrorCode.ACCESS_DENIED));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"MODERATOR", "ADMINISTRATOR"})
    void everySuspensionIsAtMostThirtyDaysForEveryRole(Role actorRole) {
        Party target = party(2, Role.MEMBER);

        assertThatCode(() -> check(party(1, actorRole), target, suspend(Duration.ofDays(30)), true))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> check(party(1, actorRole), target, suspend(Duration.ofDays(30).plusSeconds(1)), true))
                .satisfies(t -> assertCode(t, AuthErrorCode.SUSPENSION_LIMIT_EXCEEDED));
        assertThatThrownBy(() -> check(party(1, actorRole), target, suspend(Duration.ofDays(31)), true))
                .satisfies(t -> assertCode(t, AuthErrorCode.SUSPENSION_LIMIT_EXCEEDED));
    }

    @Test
    void aSuspensionMustEndInTheFuture() {
        Party moderator = party(1, Role.MODERATOR);
        Party member = party(2, Role.MEMBER);

        assertThatThrownBy(() -> check(moderator, member, suspend(Duration.ZERO), false))
                .satisfies(t -> assertCode(t, AuthErrorCode.VALIDATION_ERROR));
        assertThatThrownBy(() -> check(moderator, member, suspend(Duration.ofMinutes(-5)), false))
                .satisfies(t -> assertCode(t, AuthErrorCode.VALIDATION_ERROR));
        assertThatCode(() -> check(moderator, member, suspend(Duration.ofSeconds(1)), false))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"MEMBER", "CONTRIBUTOR", "MODERATOR"})
    void anAdministratorMayDoEverythingToALowerRoleWithoutReauthenticating(Role targetRole) {
        Party admin = party(1, Role.ADMINISTRATOR);
        Party target = party(2, targetRole);

        for (Action action : Action.values()) {
            assertThatCode(() -> check(admin, target, of(action), false)).as("%s on %s", action, targetRole)
                    .doesNotThrowAnyException();
        }
        assertThatCode(() -> check(admin, target, changeRole(Role.MODERATOR), false)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void anyActionOnAnotherAdministratorNeedsRecentReauthentication(Action action) {
        Party admin = party(1, Role.ADMINISTRATOR);
        Party other = party(2, Role.ADMINISTRATOR);

        assertThatThrownBy(() -> check(admin, other, of(action), false))
                .satisfies(t -> assertCode(t, AuthErrorCode.REAUTH_REQUIRED));
        assertThatCode(() -> check(admin, other, of(action), true)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"MEMBER", "CONTRIBUTOR", "MODERATOR"})
    void promotingToAdministratorNeedsRecentReauthentication(Role current) {
        Party admin = party(1, Role.ADMINISTRATOR);
        Party target = party(2, current);

        assertThatThrownBy(() -> check(admin, target, changeRole(Role.ADMINISTRATOR), false))
                .satisfies(t -> assertCode(t, AuthErrorCode.REAUTH_REQUIRED));
        assertThatCode(() -> check(admin, target, changeRole(Role.ADMINISTRATOR), true)).doesNotThrowAnyException();
    }

    @Test
    void theAccessCheckComesBeforeTheReauthenticationCheck() {
        // a Moderator must not learn that "re-authenticate" would unlock something that is forbidden anyway
        assertThatThrownBy(() -> check(party(1, Role.MODERATOR), party(2, Role.ADMINISTRATOR), ban(), false))
                .satisfies(t -> assertCode(t, AuthErrorCode.ACCESS_DENIED));
    }

    @Test
    void theSelfCheckComesBeforeEverythingElse() {
        assertThatThrownBy(() -> check(party(1, Role.ADMINISTRATOR), party(1, Role.ADMINISTRATOR),
                changeRole(Role.MEMBER), false)).satisfies(t -> assertCode(t, AuthErrorCode.SELF_ACTION_FORBIDDEN));
    }

    @Test
    void changingToTheSameRoleIsNotAPromotionOrAnOverride() {
        assertThatCode(() -> check(party(1, Role.ADMINISTRATOR), party(2, Role.MODERATOR), changeRole(Role.MODERATOR),
                false)).doesNotThrowAnyException();
    }
}
