# Feature Specification: User Authentication & Roles

**Feature Branch**: `feature/SCRUM-xx-user-authentication-roles` *(Jira key pending)*

**Created**: 2026-10-02

**Status**: Ready for Implementation (Sprint 02)

**Input**: User description: "Describe the login feature: user stories, acceptance criteria and edge
cases. A good spec has a summary + user stories + acceptance criteria + functional and
non-functional requirements. Write it in English."

## Summary

WikiCook needs a trustworthy way for people to create an account, sign in, stay signed in, and
recover access when they forget their password. Every signed-in person has exactly one **role**
(Member, Contributor, Moderator, Administrator) that decides what they may do on the platform.
Visitors who are not signed in (Guests) can still browse and read public recipes.

This feature is the foundation for every personalized module in the proposal (dietary profile,
meal planner, shopping list, bookmarks, reviews, recipe submission and the admin dashboard): those
modules rely on knowing *who* the user is and *what they are allowed to do*.

**In scope**: email/password registration, email verification, sign-in, sign-out, "remember me",
password reset, Google sign-in, account lockout after repeated failures, role assignment and
role-based access, suspended/banned account handling.

**Out of scope** (separate specs): editing the display name, changing or setting a password while
signed in, deleting one's own account, and dietary profile & avatar management — all of these
belong to `specs/011-user-profile-dietary` (Module 3.1 profile part); Apple sign-in, two-factor
authentication, the admin dashboard UI itself (Module 3.10).

## Clarifications

### Session 2026-10-18

- Q: (Maintainer instruction) Which scope boundary with spec 011? → A: Editing the display name, changing/setting a password, deleting the account and the dietary profile are owned by spec 011; Out of scope updated.
- Q: (Maintainer instruction) Who can see personal data (NFR-009)? → A: Email is visible only to the account owner and Administrators; display name and avatar are public (as defined in spec 011).
- Q: (Maintainer instruction) Does spec 011 need a "deleted" account status? → A: No. Spec 011 removes the account completely, so no "deleted" status is added to User Account; a hashed-email block list entity is added instead (see Key Entities).
- Q: (Maintainer instruction) Jira key in Feature Branch? → A: SCRUM-27 was the Sprint Planning task, not this feature; branch line now `feature/SCRUM-xx-…` pending a new Jira key; Assumption updated.
- Q: After an account is deleted (spec 011), can its email be used to register again immediately? → A: Yes, immediately as a brand-new independent account (FR-002, FR-026). Exceptions: a banned account may still delete itself but its email hash goes on a block list and re-registration with that email is refused; a temporarily suspended account cannot delete itself until the suspension ends. Audit entries are kept the full 90 days (FR-025) in anonymised form.

- Q: When someone signs in with Google using an email that belongs to an unverified email/password account, what happens to that account? → A: It is treated as a new account: all data created before verification is deleted (display name, dietary profile of 011, recipe drafts of 060, collections of 080), the old password is disabled, earlier sessions end, the display name comes from Google, and the account is marked verified. Reason: the real email owner must not inherit an allergy profile or content pre-set by someone else (FR-019).
- Q: How long is an account that never verifies its email kept? → A: 7 days from registration (a new verification link does not extend it); then it is deleted completely with all its data, the email can register again immediately and is not put on the block list (FR-005).
- Q: Are users notified when an Administrator changes their role or suspends/bans them? → A: Suspension/ban: email with reason, end date (suspensions) and appeal instructions, plus an email when the suspension ends or the ban is lifted. Role change: no email, but an in-app notice at the next sign-in names the new role and changed permissions (especially on demotion). Appeals themselves belong to spec 100 (FR-024a).
- Q: Can an Administrator be suspended, banned or demoted by another Administrator? → A: Yes, but never by themselves, and at least one active Administrator (not suspended, banned or deleted) must always remain; same rule as spec 011 self-deletion. A Moderator cannot act on a Moderator or Administrator (only a higher role acts on a lower one). Actions on another Administrator require re-authentication and are always audited (FR-022a, FR-023).

- Session 2026-10-19: Concurrent-user load condition changed from 200 to 100 to match the RC acceptance gate in docs/management/project-plan.md §4.4.2 and Vision Document §7 (SCRUM-36).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign in with email and password (Priority: P1)

As a **registered home cook**, I want to sign in with my email and password so that I can access
my saved recipes, meal plans and shopping lists from any device.

**Why this priority**: Sign-in is the gate to every personalized feature. Without it no other
member-only module can be demonstrated.

**Independent Test**: Using a pre-created, verified account, open the sign-in page, enter valid
credentials and confirm the user lands on their home page showing their name; then sign out and
confirm member-only pages are no longer reachable.

**Acceptance Scenarios**:

1. **Given** a verified, active account, **When** the user enters the correct email and password
   and submits, **Then** the user is signed in and redirected to the page they originally
   requested (or the home page if none).
2. **Given** a verified, active account, **When** the user enters a wrong password, **Then** sign-in
   is refused with the generic message "Email or password is incorrect" and the password field
   is cleared.
3. **Given** an email that is not registered, **When** the user tries to sign in, **Then** the same
   generic message as scenario 2 is shown (no hint that the email does not exist).
4. **Given** the user is signed in, **When** they choose "Sign out", **Then** their session ends
   immediately and pressing the browser Back button does not show member-only content.
5. **Given** the user ticks "Remember me" when signing in, **When** they close and reopen the
   browser within 30 days, **Then** they are still signed in.
6. **Given** the user did not tick "Remember me", **When** they are inactive for 24 hours or close
   the browser, **Then** they must sign in again.
7. **Given** the email is typed with different letter case or surrounding spaces
   (e.g. " An@Mail.com "), **When** the user signs in, **Then** it matches the registered email.

---

### User Story 2 - Register a new account and verify email (Priority: P1)

As a **new visitor**, I want to create an account with my email and a password so that I can start
saving recipes and planning meals.

**Why this priority**: Users must exist before they can sign in; registration and sign-in together
form the minimum viable slice.

**Independent Test**: From the sign-up page, register with a new email, open the verification link
received by email, and sign in successfully with the new credentials.

**Acceptance Scenarios**:

1. **Given** an email that is not yet registered, **When** the visitor submits a display name,
   email, password and password confirmation that meet the rules, **Then** the account is created
   with the **Member** role and a verification email is sent.
2. **Given** an email that is already registered, **When** the visitor tries to register with it,
   **Then** registration is refused with a message suggesting to sign in or reset the password.
3. **Given** a password that does not meet the password rules (FR-004), **When** the visitor submits,
   **Then** the form shows which rule failed and no account is created.
4. **Given** a newly created, unverified account, **When** the user opens the verification link
   within 24 hours, **Then** the account becomes verified.
5. **Given** an unverified account, **When** the user signs in, **Then** they are signed in but see a
   reminder to verify their email, and actions that publish content (submitting recipes, reviews,
   cooksnaps) are blocked until verification.
6. **Given** an expired verification link, **When** the user opens it, **Then** they are told the
   link expired and can request a new one.

---

### User Story 3 - Role-based access (Priority: P1)

As the **platform**, I need every signed-in person to have a role so that only authorized people
can perform moderation and administration actions.

**Why this priority**: Without roles, any member could moderate content or manage users — a direct
security risk, and a prerequisite for the Admin Dashboard module.

**Independent Test**: Sign in with one account per role and attempt one representative action for
each role; confirm allowed actions succeed and disallowed ones are refused.

**Acceptance Scenarios**:

1. **Given** a Guest (not signed in), **When** they open a public recipe, **Then** they can read it;
   **When** they try to bookmark, review or open the meal planner, **Then** they are asked to sign
   in and returned to the same place afterwards.
2. **Given** a Member, **When** they try to open a moderation or administration page, **Then** access
   is refused with an "access denied" message and nothing is revealed about the page's content.
3. **Given** a Moderator, **When** they open the recipe moderation queue, **Then** access is granted;
   **When** they try to change another user's role, **Then** access is refused.
4. **Given** an Administrator, **When** they change a user's role, **Then** the new role takes effect
   no later than that user's next request, and the change is recorded in the audit log.
5. **Given** a user whose role was lowered while signed in, **When** they next try a privileged
   action, **Then** it is refused.

---

### User Story 4 - Reset a forgotten password (Priority: P2)

As a **registered user who forgot my password**, I want to reset it through my email so that I can
regain access without contacting support.

**Why this priority**: Very common need, but sign-in and registration can be demonstrated without it.

**Independent Test**: Request a reset for an existing account, open the emailed link, set a new
password, then sign in with the new password and confirm the old one no longer works.

**Acceptance Scenarios**:

1. **Given** any email address, **When** the user requests a password reset, **Then** the system
   always shows "If this email is registered, a reset link has been sent" (no account enumeration).
2. **Given** a valid reset link less than 30 minutes old, **When** the user sets a new password that
   meets the rules, **Then** the password is changed, the link becomes unusable, and all other
   sessions of that user are signed out.
3. **Given** a reset link that was already used or is older than 30 minutes, **When** it is opened,
   **Then** the user is told it is invalid and can request a new one.
4. **Given** several reset requests in a row, **When** the user uses the most recent link, **Then**
   it works and all earlier links are invalid.

---

### User Story 5 - Sign in with Google (Priority: P3)

As a **busy home cook**, I want to sign in with my Google account so that I do not need to remember
another password.

**Why this priority**: Convenience feature listed in the proposal; email/password already covers
the core need.

**Independent Test**: Choose "Continue with Google" with a Google account not yet known to WikiCook,
confirm a Member account is created and signed in; sign out and repeat to confirm the same account
is reused.

**Acceptance Scenarios**:

1. **Given** a Google account whose email is not registered, **When** the user continues with
   Google and grants consent, **Then** a verified Member account is created and the user is
   signed in.
2. **Given** a Google account whose email matches an existing **verified** WikiCook account, **When**
   the user continues with Google, **Then** they are signed in to that existing account (same data,
   same role). If the matching account is **unverified**, it is reset as a new account (FR-019).
3. **Given** the user cancels or denies consent on the Google screen, **When** they return,
   **Then** they are back on the sign-in page with a neutral message and no account is created.

---

### Edge Cases

- **Brute force**: after 5 consecutive failed sign-in attempts on one account within 15 minutes,
  the account is temporarily locked for 15 minutes; the user sees a message stating when they can
  retry and receives an email notification. A successful sign-in resets the counter.
- **Suspended / banned account**: correct credentials still do not sign the user in; they see a
  message that the account is suspended (with end date if temporary) or banned. Active sessions
  of a user who gets suspended end on their next request.
- **Already signed in** user opens the sign-in or sign-up page → redirected to the home page.
- **Session expires while filling a form** (e.g. writing a recipe) → user is asked to sign in again
  and the unsaved form content is not silently lost (user is warned before leaving).
- **Redirect after sign-in** only goes to pages inside WikiCook; links pointing to external sites
  are ignored and the user goes to the home page.
- **Email with leading/trailing spaces or mixed case** is normalized for both sign-up and sign-in.
- **Password with leading/trailing spaces** is kept exactly as typed (not trimmed).
- **Very long input** (email > 254 characters, password > 128 characters, display name > 50 characters) is rejected with a clear validation message.
- **Double-click on submit** does not create two accounts or send two emails.
- **Google account without an email address** or with an unverified email → sign-in refused
  with a message suggesting email/password registration.
- **Last active Administrator** cannot be demoted, suspended, banned or deleted, and no
  Administrator can do any of this to themselves, so the platform never ends up with zero active
  administrators (FR-022a, FR-023).
- **Email delivery failure** (verification or reset) → user can request the email again, limited
  to 3 requests per 15 minutes per email address.
- **Unverified account reaches 7 days** → deleted automatically with all its data (FR-005); if the
  user is signed in at that moment, their next request ends the session. The same data-deletion
  event as the Google reset (Integration Points) is sent to specs 011, 060 and 080.
- **Google sign-in matches an unverified email/password account** → the old account is reset to a
  brand-new one: everything created before verification (display name, dietary profile, recipe
  drafts, collections, any other data) is deleted, the old password is disabled, all earlier
  sessions are ended, the display name is taken from the Google account, and the account becomes
  verified (FR-019). This stops a third party from pre-loading an email owner's account with
  data (e.g. an allergy profile) or a password.
- **Simultaneous sessions** on multiple devices are allowed; signing out on one device does not
  sign out the others (except after a password reset, see US4-2).

## Requirements *(mandatory)*

### Functional Requirements

**Registration & verification**

- **FR-001**: System MUST let a visitor register with display name, email, password and password
  confirmation.
- **FR-002**: System MUST treat emails as case-insensitive and trim surrounding spaces; each email
  can belong to only one account. An email whose account was deleted (spec 011) becomes free
  immediately, except emails on the Blocked Email list (FR-026).
- **FR-003**: System MUST assign the **Member** role to every newly registered account.
- **FR-004**: Passwords MUST be 8–128 characters and contain at least one letter and one digit;
  the system MUST reject passwords that equal the user's email.
- **FR-005**: System MUST send a verification email whose link is single-use and expires after
  24 hours, and MUST let the user request a new link. An account that is still unverified 7 days
  after registration MUST be deleted automatically and completely (account and all its data); the
  7 days are counted from registration and requesting a new link does not extend them. The email is
  then free to register again immediately and is NOT added to the Blocked Email list.
- **FR-006**: Unverified accounts MUST be able to sign in but MUST NOT be able to publish content
  (recipes, reviews, cooksnaps, comments).

**Sign-in & session**

- **FR-007**: System MUST authenticate users by email and password.
- **FR-008**: System MUST show one generic error for wrong email or wrong password.
- **FR-009**: System MUST offer "Remember me": checked → session lasts up to 30 days; unchecked →
  session ends after 24 hours of inactivity or when the browser is closed.
- **FR-010**: Users MUST be able to sign out; signing out MUST end the session immediately.
- **FR-011**: After sign-in, the system MUST return the user to the originally requested internal
  page, or to the home page.
- **FR-012**: System MUST lock an account for 15 minutes after 5 consecutive failed sign-in
  attempts within 15 minutes and notify the account owner by email.
- **FR-013**: System MUST refuse sign-in for suspended or banned accounts and display the reason
  category and, for suspensions, the end date.

**Password reset**

- **FR-014**: Users MUST be able to request a password reset by email; the response MUST be the
  same whether or not the email is registered.
- **FR-015**: Reset links MUST be single-use, expire after 30 minutes, and be invalidated when a
  newer link is issued.
- **FR-016**: A successful password reset MUST sign the user out of all other sessions.
- **FR-017**: System MUST limit verification and reset email requests to 3 per 15 minutes per
  email address.

**Google sign-in**

- **FR-018**: Users MUST be able to sign in with a Google account that has a verified email.
- **FR-019**: If the Google email matches an existing **verified** account, the system MUST link and
  sign in to that account rather than create a duplicate. If it matches an **unverified** account,
  the system MUST treat it as a new account: delete all data created before verification, disable
  the old password, end all earlier sessions, take the display name from the Google account and mark
  the account verified.

**Roles & access control**

- **FR-020**: Each account MUST have exactly one role: Member, Contributor, Moderator or
  Administrator. Guests are visitors without an account.
- **FR-021**: System MUST enforce the permission matrix below for every protected action, on every
  request (not only by hiding buttons).
- **FR-022**: Only Administrators MUST be able to change a user's role or suspend/ban an account.
- **FR-022a**: Only a higher role may act on a lower role. A Moderator MUST NOT suspend, ban or
  change the role of another Moderator or of an Administrator. An Administrator MAY act on another
  Administrator, but MUST NOT suspend, ban, demote or delete themselves. Any action on another
  Administrator MUST require the acting Administrator to re-authenticate and MUST always be
  audited (FR-025).
- **FR-023**: System MUST prevent removing the Administrator role from, suspending, banning or
  deleting the last remaining **active** Administrator. "Active Administrator" means an
  Administrator account that is not suspended, banned or deleted; the system MUST always keep at
  least one. Spec 011 uses the same rule for self-deletion.
- **FR-024**: Role changes, suspensions and bans MUST take effect no later than the affected
  user's next request.
- **FR-024a**: System MUST email the account owner when the account is suspended or banned, stating
  the reason, the end date (for suspensions) and how to appeal; it MUST also email them when a
  suspension ends or a ban is lifted. A role change MUST NOT send an email; instead, at the user's
  next sign-in (or next request if already signed in) the app MUST show a notice naming the new
  role and the permissions that changed, stressing any permissions lost on a demotion. The notice
  is shown once and then dismissed.
- **FR-025**: System MUST record an audit entry (who, what, when, target account) for sign-in
  successes and failures, lockouts, password resets, role changes, suspensions and bans. When the
  target account is deleted, its audit entries MUST be kept for the full retention period (NFR-008)
  in anonymised form (no email or display name).

**Account deletion rules** *(deletion itself is specified in spec 011)*

- **FR-026**: A user whose account is **banned** MAY still delete their account, but the system
  MUST then store only a hash of the email on a block list (never the plain email) and MUST refuse
  registration and Google sign-in creation with any email matching it. A user who is **temporarily
  suspended** MUST NOT be able to delete their account until the suspension ends. For all other
  deleted accounts nothing is kept on the block list.

**Permission matrix** (✓ = allowed)

| Action                                              | Guest | Member | Contributor | Moderator | Administrator |
|-----------------------------------------------------|:-----:|:------:|:-----------:|:---------:|:-------------:|
| Browse and read published recipes                   |   ✓   |   ✓    |      ✓      |     ✓     |       ✓       |
| Bookmark, meal plan, shopping list                  |       |   ✓    |      ✓      |     ✓     |       ✓       |
| Review, rate, post cooksnaps *(verified email)*     |       |   ✓    |      ✓      |     ✓     |       ✓       |
| Submit recipes *(goes to moderation queue)*         |       |   ✓    |      ✓      |     ✓     |       ✓       |
| "Verified Contributor" badge on own recipes         |       |        |      ✓      |           |               |
| Moderate recipes, reviews and user reports          |       |        |             |     ✓     |       ✓       |
| Manage categories and taxonomies                    |       |        |             |           |       ✓       |
| Change roles, suspend or ban users                  |       |        |             |           |       ✓       |

### Non-Functional Requirements

- **NFR-001 (Security – credentials)**: Passwords MUST never be stored or logged in readable form
  and MUST never be shown back to the user or to administrators.
- **NFR-002 (Security – transport)**: All authentication pages and requests MUST be served over an
  encrypted connection.
- **NFR-003 (Security – privacy)**: Error messages and response timing MUST NOT reveal whether an
  email is registered (sign-in, reset).
- **NFR-004 (Performance)**: 95% of sign-in attempts MUST complete in under 2 seconds under normal
  load (up to 100 concurrent users).
- **NFR-005 (Availability)**: Sign-in MUST remain available when the Google sign-in provider is
  unreachable (email/password still works).
- **NFR-006 (Usability)**: Sign-in and sign-up pages MUST be fully usable on screens from 360 px
  wide, with Vietnamese as the default interface language.
- **NFR-007 (Accessibility)**: Forms MUST be operable by keyboard only, every field MUST have a
  visible label, and error messages MUST be announced next to the related field.
- **NFR-008 (Auditability)**: Audit entries (FR-025) MUST be retained for at least 90 days and be
  viewable by Administrators.
- **NFR-009 (Data protection)**: A user's email MUST be visible only to the account owner and
  Administrators. Display name and avatar are public (as defined in spec 011).

### Key Entities

- **User Account**: a person registered on WikiCook — display name, email (unique), verification
  status, account status (active / locked until / suspended until / banned), role, created date,
  last sign-in date.
- **Role**: one of Member, Contributor, Moderator, Administrator; defines the permissions in the
  matrix above. One account has exactly one role.
- **Linked External Identity**: a Google identity connected to a User Account (provider, provider
  user identifier, email). One account may have zero or one Google identity.
- **Session**: a signed-in period on one device — owner account, created time, last activity,
  "remember me" flag, expiry.
- **One-time Token**: an email verification or password reset link — owner account, purpose,
  expiry, used/unused.
- **Audit Entry**: a security-relevant event — actor, action, target account, time, outcome. After
  the target account is deleted, the entry is kept but anonymised.
- **Blocked Email**: a one-way hash (never the plain email) of the email of a banned account
  that deleted itself; used only to refuse re-registration.

*No "deleted" account status exists: spec 011 removes the User Account completely, so a deleted
account is simply absent (its public content shows "Người dùng đã xóa").*

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new visitor can register and reach the verified, signed-in state in under 3 minutes
  (excluding email delivery delay).
- **SC-002**: A returning user can sign in in under 15 seconds from opening the sign-in page.
- **SC-003**: At least 90% of test participants complete sign-up and sign-in on their first attempt
  without help.
- **SC-004**: 100% of protected actions in the permission matrix are refused for unauthorized roles
  in acceptance testing (zero privilege-escalation defects).
- **SC-005**: 0 accounts can be accessed after 5 wrong passwords within 15 minutes (lockout works in
  100% of tests).
- **SC-006**: 95% of users who request a password reset can sign in again within 5 minutes.
- **SC-007**: Every acceptance scenario in this spec has at least one passing automated test.

## Assumptions

- The initial Administrator account is created by the development team during deployment; there is
  no self-registration for Moderator or Administrator.
- "Verified Contributor" is granted manually by an Administrator (criteria defined in the Admin
  Dashboard spec).
- Apple sign-in, listed in the proposal, is deferred because it requires a paid Apple developer
  account; it can be added later without changing roles or sessions.
- Two-factor authentication is not required for this course project.
- An email-sending service is available for verification, reset and lockout notification emails.
- Dietary profile, avatar, household size, display-name editing, password change/set and account
  deletion are covered by `specs/011-user-profile-dietary`, which depends on this one.
- SCRUM-27 is the Sprint Planning task on Jira, not this feature. A new Jira key for this feature
  is not yet assigned; once created, replace `SCRUM-xx` in the branch name per the constitution
  (Principle I).

## Integration Points

- **Spec 011 – account deletion flow**: 011 performs the deletion (re-authentication, typing
  `DELETE`, anonymising content). 010 adds: (1) a banned user may delete, and 011 MUST call 010 to
  store the email hash on the Blocked Email list; (2) a temporarily suspended user is refused until
  the suspension ends; (3) the email is freed immediately otherwise; (4) audit entries are
  anonymised, not deleted (FR-025/FR-026).
- **Specs 011, 060, 080 – reset of an unverified account on Google sign-in (FR-019)**: 010 emits
  an "account reset" event when Google sign-in takes over an unverified account. 011 MUST delete
  the dietary profile (and bio/avatar), 060 MUST delete the user's recipe drafts and any other
  recipes, and 080 MUST delete the user's collections and bookmarks; none of them may keep data
  created before verification. Data created after verification is never affected.
- **Specs 011 and 100 – active Administrator rule (FR-022a, FR-023)**: "Active Administrator" =
  not suspended, banned or deleted. 011 MUST refuse self-deletion of the last active Administrator
  using this rule. 100 MUST enforce the role hierarchy (Moderator cannot act on Moderator or
  Administrator; Administrator may act on Administrator but never on themselves), require
  re-authentication before any action on another Administrator, refuse any action that would leave
  zero active Administrators, and always write an audit entry.
- **Spec 100 – ban / suspension and appeals**: 100 owns the moderation actions and the appeal
  process; 010 owns the account status, the Blocked Email list and sending the suspension/ban,
  end-of-suspension and ban-lifted emails (FR-024a). 100 MUST supply the reason, the end date and
  the appeal link/instructions for each email, and MUST notify 010 when a suspension expires or a
  ban is lifted. Role-change notices are in-app only. Ban and suspend actions MUST use FR-022/FR-024, and 100 reads the
  block list only through 010 (registration refusal), never the plain email.
