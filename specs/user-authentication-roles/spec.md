# Feature Specification: User Authentication & Roles

**Feature Branch**: `feature/SCRUM-xx-user-authentication-roles` *(TODO: assign Jira key)*

**Created**: 2026-10-02

**Status**: Draft

**Input**: User description: "Describe the login feature: user stories, acceptance criteria and edge
cases. A good spec has a summary + user stories + acceptance criteria + functional and
non-functional requirements. Write it in English."

## Summary

WikiCook needs a trustworthy way for people to create an account, sign in, stay signed in, and
recover access when they forget their password. Every signed-in person has exactly one **role**
(Member, Contributor, Moderator, Administrator) that decides what they may do on the platform.
Visitors who are not signed in (Guests) can still browse and read public recipes.

This feature is the foundation for every personalised module in the proposal (dietary profile,
meal planner, shopping list, bookmarks, reviews, recipe submission and the admin dashboard): those
modules rely on knowing *who* the user is and *what they are allowed to do*.

**In scope**: email/password registration, email verification, sign-in, sign-out, "remember me",
password reset, Google sign-in, account lockout after repeated failures, role assignment and
role-based access, suspended/banned account handling.

**Out of scope** (separate specs): dietary profile & avatar management (Module 3.1 profile part),
Apple sign-in, two-factor authentication, the admin dashboard UI itself (Module 3.10).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign in with email and password (Priority: P1)

As a **registered home cook**, I want to sign in with my email and password so that I can access
my saved recipes, meal plans and shopping lists from any device.

**Why this priority**: Sign-in is the gate to every personalised feature. Without it no other
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

As the **platform**, I need every signed-in person to have a role so that only authorised people
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
2. **Given** a Google account whose email matches an existing WikiCook account, **When** the user
   continues with Google, **Then** they are signed in to that existing account (same data, same role).
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
- **Email with leading/trailing spaces or mixed case** is normalised for both sign-up and sign-in.
- **Password with leading/trailing spaces** is kept exactly as typed (not trimmed).
- **Very long input** (email > 254 characters, password > 128 characters, display name
  > 50 characters) is rejected with a clear validation message.
- **Double-click on submit** does not create two accounts or send two emails.
- **Google account without an email address** or with an unverified email → sign-in refused
  with a message suggesting email/password registration.
- **Last Administrator** cannot remove their own Administrator role or be demoted, so the platform
  never ends up with zero administrators.
- **Email delivery failure** (verification or reset) → user can request the email again, limited
  to 3 requests per 15 minutes per email address.
- **Simultaneous sessions** on multiple devices are allowed; signing out on one device does not
  sign out the others (except after a password reset, see US4-2).

## Requirements *(mandatory)*

### Functional Requirements

**Registration & verification**

- **FR-001**: System MUST let a visitor register with display name, email, password and password
  confirmation.
- **FR-002**: System MUST treat emails as case-insensitive and trim surrounding spaces; each email
  can belong to only one account.
- **FR-003**: System MUST assign the **Member** role to every newly registered account.
- **FR-004**: Passwords MUST be 8–128 characters and contain at least one letter and one digit;
  the system MUST reject passwords that equal the user's email.
- **FR-005**: System MUST send a verification email whose link is single-use and expires after
  24 hours, and MUST let the user request a new link.
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
- **FR-019**: If the Google email matches an existing account, the system MUST link and sign in to
  that account rather than create a duplicate.

**Roles & access control**

- **FR-020**: Each account MUST have exactly one role: Member, Contributor, Moderator or
  Administrator. Guests are visitors without an account.
- **FR-021**: System MUST enforce the permission matrix below for every protected action, on every
  request (not only by hiding buttons).
- **FR-022**: Only Administrators MUST be able to change a user's role or suspend/ban an account.
- **FR-023**: System MUST prevent removing the Administrator role from the last remaining
  Administrator.
- **FR-024**: Role changes, suspensions and bans MUST take effect no later than the affected
  user's next request.
- **FR-025**: System MUST record an audit entry (who, what, when, target account) for sign-in
  successes and failures, lockouts, password resets, role changes, suspensions and bans.

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
  load (up to 200 concurrent users).
- **NFR-005 (Availability)**: Sign-in MUST remain available when the Google sign-in provider is
  unreachable (email/password still works).
- **NFR-006 (Usability)**: Sign-in and sign-up pages MUST be fully usable on screens from 360 px
  wide, with Vietnamese as the default interface language.
- **NFR-007 (Accessibility)**: Forms MUST be operable by keyboard only, every field MUST have a
  visible label, and error messages MUST be announced next to the related field.
- **NFR-008 (Auditability)**: Audit entries (FR-025) MUST be retained for at least 90 days and be
  viewable by Administrators.
- **NFR-009 (Data protection)**: Users' personal data collected here (email, display name) MUST be
  visible only to the account owner and Administrators.

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
- **Audit Entry**: a security-relevant event — actor, action, target account, time, outcome.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new visitor can register and reach the verified, signed-in state in under 3 minutes
  (excluding email delivery delay).
- **SC-002**: A returning user can sign in in under 15 seconds from opening the sign-in page.
- **SC-003**: At least 90% of test participants complete sign-up and sign-in on their first attempt
  without help.
- **SC-004**: 100% of protected actions in the permission matrix are refused for unauthorised roles
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
- Dietary profile, avatar and household size (Module 3.1 profile part) are covered by a separate
  "user profile" spec that depends on this one.
- Jira key for this feature is not yet assigned; it must be added to the branch name per the
  constitution (Principle I).
